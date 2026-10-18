package com.example.doc_extractor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** מגדיר את הגישה למסד הנתונים של חשבוניות. */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByVendorContainingIgnoreCase(String vendor);
}