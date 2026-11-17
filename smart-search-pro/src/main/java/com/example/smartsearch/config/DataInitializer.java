package com.example.smartsearch.config;

import com.example.smartsearch.document.DocumentEntity;
import com.example.smartsearch.document.DocumentRepository;
import com.example.smartsearch.search.LuceneSearchService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean ApplicationRunner seedExamples(DocumentRepository repository, LuceneSearchService search) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new DocumentEntity("Java Programming", "Programming", "Java is an object-oriented programming language used to build reliable applications. Classes, objects, inheritance and collections are core concepts.", "sample-java.txt"));
                repository.save(new DocumentEntity("Data Structures", "Computer Science", "Data structures organize and store data efficiently. Common examples include arrays, linked lists, stacks, queues, trees and graphs.", "sample-data-structures.txt"));
                repository.save(new DocumentEntity("Artificial Intelligence", "Technology", "Artificial intelligence enables computers to perform tasks such as learning from data, reasoning, prediction and language processing.", "sample-ai.txt"));
                repository.save(new DocumentEntity("Database Management", "Database", "Database management systems store, organize, retrieve and protect structured information using queries and transactions.", "sample-database.txt"));
            }
            search.rebuildIndex();
        };
    }
}
