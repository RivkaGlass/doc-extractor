package com.example.doc_extractor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ExtractionService {

    private static final String PROMPT =
            "Extract vendor, invoice date (YYYY-MM-DD), total amount (number only) and "
          + "currency (ISO code) from this document. Return a single JSON object, not an array, "
          + "with the keys: vendor, invoiceDate, total, currency. "
          + "Use null for anything you cannot find.";

    private final DocumentRepository repository;
    private final RestClient client = RestClient.create("https://generativelanguage.googleapis.com");
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String apiKey;
    private final String model;

    public ExtractionService(DocumentRepository repository,
                             @Value("${GEMINI_API_KEY}") String apiKey,
                             @Value("${gemini.model:gemini-2.5-flash-lite}") String model) {
        this.repository = repository;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Document extract(MultipartFile file) {
        String mediaType = file.getContentType();
        if (mediaType == null || !mediaType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are supported");
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());

            // Forces Gemini to return exactly one object with these four fields
            Map<String, Object> schema = Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "vendor", Map.of("type", "STRING", "nullable", true),
                            "invoiceDate", Map.of("type", "STRING", "nullable", true),
                            "total", Map.of("type", "NUMBER", "nullable", true),
                            "currency", Map.of("type", "STRING", "nullable", true)),
                    "required", List.of("vendor", "invoiceDate", "total", "currency"));

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(
                            Map.of("inline_data", Map.of("mime_type", mediaType, "data", base64)),
                            Map.of("text", PROMPT)))),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseSchema", schema,
                            "temperature", 0));

            Map<?, ?> response = client.post()
                    .uri("/v1beta/models/" + model + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> contentObj = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            List<?> parts = (List<?>) contentObj.get("parts");
            String text = (String) ((Map<?, ?>) parts.get(0)).get("text");
            text = text.replace("```json", "").replace("```", "").trim();

            // Fallback: accept both an object and an array (takes the first item)
            Object parsed = mapper.readValue(text, Object.class);
            Map<?, ?> fields = (parsed instanceof List<?> list && !list.isEmpty())
                    ? (Map<?, ?>) list.get(0)
                    : (Map<?, ?>) parsed;

            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setImageData(file.getBytes());
            doc.setImageType(mediaType);
            doc.setVendor(asString(fields.get("vendor")));
            doc.setCurrency(asString(fields.get("currency")));

            String date = asString(fields.get("invoiceDate"));
            if (date != null) {
                try { doc.setInvoiceDate(LocalDate.parse(date)); } catch (Exception ignored) { }
            }
            String total = asString(fields.get("total"));
            if (total != null) {
                try { doc.setTotal(new BigDecimal(total.replace(",", ""))); } catch (Exception ignored) { }
            }

            return repository.save(doc);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Extraction failed: " + e);
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}