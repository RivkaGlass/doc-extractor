package com.example.doc_extractor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentExtractController {

    private final ExtractionService service;

    public DocumentExtractController(ExtractionService service) {
        this.service = service;
    }

    @PostMapping("/extract")
    @ResponseStatus(HttpStatus.CREATED)
    public Document extract(@RequestParam("file") MultipartFile file) {
        return service.extract(file);
    }
}
