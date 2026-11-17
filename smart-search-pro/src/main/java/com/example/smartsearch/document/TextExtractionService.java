package com.example.smartsearch.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class TextExtractionService {
    public String extract(String filename, byte[] bytes) throws IOException {
        String name = filename == null ? "upload.txt" : filename;
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")) return new String(bytes, StandardCharsets.UTF_8);
        if (lower.endsWith(".pdf")) {
            try (var pdf = Loader.loadPDF(bytes)) { return new PDFTextStripper().getText(pdf); }
        }
        if (lower.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes)); XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) { return extractor.getText(); }
        }
        throw new IllegalArgumentException("Unsupported file type. Upload TXT, MD, CSV, PDF, or DOCX.");
    }
}
