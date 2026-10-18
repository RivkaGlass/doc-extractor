package com.example.doc_extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Set;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private static final Set<String> ALLOWED = Set.of(
            "image/png", "image/jpeg", "image/webp", "application/pdf");

    private static final String PROMPT = """
            You are extracting data from an invoice (image or PDF).
            Return a single JSON object, not an array, with the keys: vendor, invoiceDate, total, currency.
            - vendor: the business that ISSUED the invoice (not the customer).
            - invoiceDate: the invoice ISSUE date as YYYY-MM-DD. Not the due date or payment date.
              Dates on Israeli invoices are written DD/MM/YYYY (day first).
            - total: the final total including VAT, as a number only.
            - currency: ISO code (ILS, USD, EUR). Map the shekel sign to ILS.
            If a field is not clearly visible, use null. Never guess.
            """;

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
        String mediaType = resolveMime(file);
        if (!ALLOWED.contains(mediaType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + mediaType);
        }

        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);

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
            log.info("Gemini raw result for {}: {}", file.getOriginalFilename(), text);

            // Fallback: accept both an object and an array (takes the first item)
            Object parsed = mapper.readValue(text, Object.class);
            Map<?, ?> fields = (parsed instanceof List<?> list && !list.isEmpty())
                    ? (Map<?, ?>) list.get(0)
                    : (Map<?, ?>) parsed;

            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setImageData(bytes);
            doc.setImageType(mediaType);
            doc.setVendor(asString(fields.get("vendor")));
            doc.setCurrency(asString(fields.get("currency")));

            String date = asString(fields.get("invoiceDate"));
            if (date != null) {
                try { doc.setInvoiceDate(LocalDate.parse(date)); }
                catch (Exception e) { log.warn("Could not parse date '{}'", date); }
            }
            String total = asString(fields.get("total"));
            if (total != null) {
                try { doc.setTotal(new BigDecimal(total.replace(",", ""))); }
                catch (Exception e) { log.warn("Could not parse total '{}'", total); }
            }

            return repository.save(doc);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Extraction failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Extraction failed: " + e);
        }
    }

    private static String resolveMime(MultipartFile file) {
        String type = file.getContentType();
        if (type == null || type.equals("application/octet-stream")) {
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            if (name.endsWith(".pdf")) type = "application/pdf";
            else if (name.endsWith(".png")) type = "image/png";
            else if (name.endsWith(".webp")) type = "image/webp";
            else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) type = "image/jpeg";
            else type = "application/octet-stream";
        }
        return type;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}