package com.example.smartsearch.search;
import com.example.smartsearch.document.DocumentEntity;
public record SearchHit(DocumentEntity document, float score, String snippet) {}
