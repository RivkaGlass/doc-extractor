package com.example.doc_extractor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.MediaType;
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository repository;

    public DocumentController(DocumentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Document> getAll() {
        return repository.findAll();
    }
    @GetMapping("/search")
    public List<Document> search(@RequestParam String vendor) {
        return repository.findByVendorContainingIgnoreCase(vendor);
    }
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!repository.existsById(id)) {
        return ResponseEntity.notFound().build();
    }
    repository.deleteById(id);
    return ResponseEntity.noContent().build();
}
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return repository.findById(id)
                .filter(d -> d.getImageData() != null)
                .map(d -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(d.getImageType()))
                        .body(d.getImageData()))
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}")
    public ResponseEntity<Document> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document create(@RequestBody Document document) {
        return repository.save(document);
    }
}
