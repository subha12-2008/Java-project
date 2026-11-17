package com.example.smartsearch.document;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents", indexes = {@Index(name = "idx_document_category", columnList = "category")})
public class DocumentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 250) private String title;
    @Column(nullable = false, length = 120) private String category;
    @Lob @Column(nullable = false) private String content;
    @Column(length = 255) private String originalFilename;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();

    protected DocumentEntity() {}
    public DocumentEntity(String title, String category, String content, String originalFilename) {
        this.title = title; this.category = category; this.content = content; this.originalFilename = originalFilename;
    }
    @PreUpdate public void beforeUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;} public String getTitle(){return title;} public String getCategory(){return category;}
    public String getContent(){return content;} public String getOriginalFilename(){return originalFilename;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setTitle(String title){this.title=title;} public void setCategory(String category){this.category=category;}
    public void setContent(String content){this.content=content;} public void setOriginalFilename(String name){this.originalFilename=name;}
}
