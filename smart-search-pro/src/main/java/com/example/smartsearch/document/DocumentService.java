package com.example.smartsearch.document;

import com.example.smartsearch.search.LuceneSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DocumentService {
    private final DocumentRepository repository; private final LuceneSearchService search;
    public DocumentService(DocumentRepository repository, LuceneSearchService search) { this.repository=repository; this.search=search; }
    public List<DocumentEntity> all() { return repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"updatedAt")); }
    @Transactional public DocumentEntity save(String title, String category, String content, String filename, Long id) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required.");
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Category is required.");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Document content is empty.");
        DocumentEntity entity = id == null ? new DocumentEntity(title.trim(),category.trim(),content,filename) : repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Document no longer exists."));
        entity.setTitle(title.trim()); entity.setCategory(category.trim()); entity.setContent(content); entity.setOriginalFilename(filename);
        DocumentEntity saved = repository.saveAndFlush(entity); search.index(saved); return saved;
    }
    @Transactional public void delete(Long id) { repository.deleteById(id); search.remove(id); }
    public long count() { return repository.count(); }
}
