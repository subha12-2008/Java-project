# Smart Search Pro — Java-only web application

A full-text document search website built with Java 21, Spring Boot, Vaadin (UI written in Java), Spring Data JPA, H2 database, and Apache Lucene. No hand-written JavaScript is required for the interface.

## Features
- Responsive dashboard-style GUI and search results with relevance scores.
- Full-text search backed by a Lucene inverted index; title boost 4x, category boost 2x, body text 1x.
- AND matching by default; category filter.
- Add, view, edit, delete, and download text for documents.
- Upload TXT, MD, CSV, PDF, and DOCX files; supported formats are extracted into searchable text.
- Persistent local H2 database; data remains after application restart.
- Search index rebuilds from the database on startup.
- Four example records are inserted on the first run.

## Requirements
- JDK 21
- Maven 3.9+
- Internet on first run so Maven can download dependencies

Check versions:
```bash
java -version
mvn -version
```

## Run the website
From the project folder:
```bash
mvn spring-boot:run
```
Wait for `Started SmartSearchApplication`, then open http://localhost:8080 . Stop with Ctrl+C.

Build a runnable JAR:
```bash
mvn clean package
java -jar target/smart-search-pro-1.0.0.jar
```

## How adding documents works
1. Click **Add document**.
2. Enter a title and category.
3. Either paste text into Document content or upload TXT/MD/CSV/PDF/DOCX.
4. Click **Save document**.
5. `DocumentService` stores it in H2 and calls `LuceneSearchService.index(...)`; the document can then be searched immediately.

The database file is created under `./data/`. Do not delete that folder if you want to retain local data. The Lucene index is stored in `./data/lucene-index/` and is rebuilt from the database at startup.

## Database console (local development only)
Visit http://localhost:8080/h2-console and use:
- JDBC URL: `jdbc:h2:file:./data/smartsearch;AUTO_SERVER=TRUE`
- User: `sa`
- Password: leave blank

This console is enabled for local development. Disable it before exposing the application publicly.

## Project structure
- `document/DocumentEntity.java` — JPA table model
- `document/DocumentRepository.java` — database access
- `document/DocumentService.java` — validation and CRUD orchestration
- `document/TextExtractionService.java` — extracts text from supported upload formats
- `search/LuceneSearchService.java` — inverted index, weighted search and category filter
- `ui/MainView.java` — entire website UI, written in Java with Vaadin components
- `config/DataInitializer.java` — initial demo data

## Important implementation notes
- This is a strong local/demo foundation, not a production-secured public service. Before deployment, add authentication/authorization, CSRF-aware security configuration, rate limits, malware scanning for uploads, backups, audit logging, and a managed database such as PostgreSQL.
- Uploaded files are converted to text; the original binary file is not retained. The UI stores the extracted content and original filename.
- Scanned PDFs containing only images need OCR, which is not included.
- The search engine uses Lucene's standard analyzer and relevance scoring. It is full-text search, not semantic/LLM search.
- If you change database configuration, update the H2 console JDBC URL accordingly.
