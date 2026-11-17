package com.example.smartsearch.search;

import com.example.smartsearch.document.DocumentEntity;
import com.example.smartsearch.document.DocumentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LuceneSearchService {
    private final DocumentRepository repository;
    private final Analyzer analyzer = new StandardAnalyzer();
    private final Directory directory;
    private final IndexWriter writer;

    public LuceneSearchService(DocumentRepository repository) throws IOException {
        this.repository = repository;
        Path indexPath = Paths.get("data", "lucene-index"); Files.createDirectories(indexPath);
        this.directory = FSDirectory.open(indexPath);
        this.writer = new IndexWriter(directory, new IndexWriterConfig(analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND));
    }

    @PostConstruct public void rebuildIndex() {
        try {
            writer.deleteAll();
            for (DocumentEntity item : repository.findAll()) addToIndex(item);
            writer.commit();
        } catch (IOException e) { throw new IllegalStateException("Could not rebuild search index", e); }
    }

    public synchronized void index(DocumentEntity entity) {
        try { addToIndex(entity); writer.commit(); }
        catch (IOException e) { throw new IllegalStateException("Could not index document", e); }
    }
    private void addToIndex(DocumentEntity item) throws IOException {
        org.apache.lucene.document.Document d = new org.apache.lucene.document.Document();
        d.add(new StringField("id", item.getId().toString(), Field.Store.YES));
        d.add(new TextField("title", safe(item.getTitle()), Field.Store.YES));
        d.add(new StringField("category", safe(item.getCategory()), Field.Store.YES));
        d.add(new TextField("categoryText", safe(item.getCategory()), Field.Store.NO));
        d.add(new TextField("content", safe(item.getContent()), Field.Store.NO));
        String preview = safe(item.getContent()).replaceAll("\s+", " ");
        d.add(new StoredField("preview", preview.substring(0, Math.min(420, preview.length()))));
        writer.updateDocument(new Term("id", item.getId().toString()), d);
    }
    public synchronized List<SearchHit> search(String queryText, String category, int limit) {
        if (queryText == null || queryText.isBlank()) return List.of();
        try (DirectoryReader reader = DirectoryReader.open(writer)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Map<String, Float> boosts = new LinkedHashMap<>(); boosts.put("title", 4.0f); boosts.put("categoryText", 2.0f); boosts.put("content", 1.0f);
            MultiFieldQueryParser parser = new MultiFieldQueryParser(boosts.keySet().toArray(String[]::new), analyzer, boosts);
            parser.setDefaultOperator(MultiFieldQueryParser.Operator.AND);
            Query textQuery;
            try { textQuery = parser.parse(escapeSpecialSyntax(queryText)); }
            catch (org.apache.lucene.queryparser.classic.ParseException ex) { return List.of(); }
            Query finalQuery = textQuery;
            if (category != null && !category.equalsIgnoreCase("All categories")) {
                BooleanQuery.Builder b = new BooleanQuery.Builder();
                b.add(textQuery, BooleanClause.Occur.MUST);
                b.add(new TermQuery(new Term("category", category)), BooleanClause.Occur.FILTER);
                finalQuery = b.build();
            }
            TopDocs docs = searcher.search(finalQuery, Math.max(1, Math.min(limit, 200)));
            List<SearchHit> hits = new ArrayList<>();
            for (ScoreDoc sd : docs.scoreDocs) {
                org.apache.lucene.document.Document stored = searcher.doc(sd.doc);
                long id = Long.parseLong(stored.get("id"));
                repository.findById(id).ifPresent(entity -> hits.add(new SearchHit(entity, sd.score, stored.get("preview"))));
            }
            return hits;
        } catch (IOException e) { throw new IllegalStateException("Search failed", e); }
    }
    public synchronized void remove(Long id) {
        try { writer.deleteDocuments(new Term("id", id.toString())); writer.commit(); }
        catch (IOException e) { throw new IllegalStateException("Could not remove document from index", e); }
    }
    public List<String> categories() { return repository.findAll().stream().map(DocumentEntity::getCategory).filter(Objects::nonNull).distinct().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList()); }
    private static String safe(String s) { return s == null ? "" : s; }
    // Treat user input as search text rather than allowing accidental Lucene query syntax.
    private static String escapeSpecialSyntax(String input) {
        StringBuilder out = new StringBuilder();
        for (char c : input.toCharArray()) {
            if ("+-!(){}[]^\"~*?:/".indexOf(c) >= 0) out.append('\');
            out.append(c);
        }
        return out.toString();
    }
    @PreDestroy public void close() throws IOException { writer.close(); directory.close(); analyzer.close(); }
}
