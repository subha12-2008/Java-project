package com.example.smartsearch.ui;

import com.example.smartsearch.document.*;
import com.example.smartsearch.search.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.component.upload.*;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.Component;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Route("")
@Theme("smartsearch")
@PageTitle("Smart Search Pro")
public class MainView extends VerticalLayout {
    private final DocumentService documents; private final LuceneSearchService search; private final TextExtractionService extraction;
    private final Grid<DocumentEntity> libraryGrid = new Grid<>(DocumentEntity.class, false);
    private final Grid<SearchHit> resultsGrid = new Grid<>(SearchHit.class, false);
    private final TextField query = new TextField(); private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final Span documentCount = new Span(); private final Div resultSummary = new Div();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public MainView(DocumentService documents, LuceneSearchService search, TextExtractionService extraction) {
        this.documents=documents; this.search=search; this.extraction=extraction;
        setSizeFull(); setPadding(false); setSpacing(false); addClassName("app-shell");
        add(header(), mainContent()); refreshLibrary();
    }
    private Component header() {
        HorizontalLayout bar = new HorizontalLayout(); bar.setWidthFull(); bar.setAlignItems(Alignment.CENTER); bar.addClassName("topbar");
        Div brandIcon = new Div(new H2("S")); brandIcon.addClassName("brand-icon");
        VerticalLayout brandText = new VerticalLayout(new H3("Smart Search"), new Span("PRO DOCUMENT DISCOVERY")); brandText.setPadding(false); brandText.setSpacing(false); brandText.addClassName("brand-copy");
        HorizontalLayout brand = new HorizontalLayout(brandIcon,brandText); brand.setAlignItems(Alignment.CENTER); brand.setSpacing(true);
        Button add = new Button("Add document", VaadinIcon.PLUS.create(), e -> openDocumentDialog(null)); add.addClassName("primary-button");
        bar.add(brand); bar.expand(brand); bar.add(add); return bar;
    }
    private Component mainContent() {
        VerticalLayout main = new VerticalLayout(); main.addClassName("main-content"); main.setWidthFull(); main.setPadding(true); main.setSpacing(true);
        H1 heading = new H1("Find knowledge faster."); heading.addClassName("page-heading");
        Paragraph sub = new Paragraph("Search every document by meaning-bearing words, title, or category. Your library is saved between sessions."); sub.addClassName("page-subtitle");
        HorizontalLayout stats = new HorizontalLayout(statCard("LIBRARY DOCUMENTS", documentCount), statCard("SEARCH ENGINE", new Span("Lucene full-text")), statCard("STORAGE", new Span("Persistent database"))); stats.setWidthFull(); stats.addClassName("stats-row");
        query.setPlaceholder("Try: Java collections, artificial intelligence..."); query.setPrefixComponent(VaadinIcon.SEARCH.create()); query.setClearButtonVisible(true); query.setWidthFull(); query.addClassName("search-input");
        categoryFilter.setLabel("Category"); categoryFilter.setItems("All categories"); categoryFilter.setValue("All categories"); categoryFilter.setClearButtonVisible(false); categoryFilter.setWidth("220px");
        Button go = new Button("Search", VaadinIcon.SEARCH.create(), e -> runSearch()); go.addClassName("primary-button"); query.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> runSearch());
        HorizontalLayout searchBar = new HorizontalLayout(query,categoryFilter,go); searchBar.setWidthFull(); searchBar.setAlignItems(Alignment.END); searchBar.expand(query); searchBar.addClassName("search-bar");
        resultSummary.addClassName("result-summary"); configureResults();
        VerticalLayout searchPanel = panel(new H3("Search workspace"), new Paragraph("Title is weighted most heavily, followed by category and body text."), searchBar, resultSummary, resultsGrid); searchPanel.addClassName("search-panel");
        configureLibrary();
        VerticalLayout libraryPanel = panel(new H3("Your document library"), new Paragraph("Upload files or add notes manually. Edit or delete records at any time."), libraryGrid); libraryPanel.addClassName("library-panel");
        main.add(heading,sub,stats,searchPanel,libraryPanel); return main;
    }
    private Component statCard(String label, Component value) { VerticalLayout card=new VerticalLayout(new Span(label),value); card.setPadding(true); card.setSpacing(false); card.addClassName("stat-card"); return card; }
    private VerticalLayout panel(Component... components) { VerticalLayout p=new VerticalLayout(components); p.setWidthFull(); p.setPadding(true); p.setSpacing(true); p.addClassName("surface-panel"); return p; }
    private void configureResults() {
        resultsGrid.addClassName("data-grid"); resultsGrid.setAllRowsVisible(true); resultsGrid.addColumn(h -> h.document().getTitle()).setHeader("DOCUMENT").setAutoWidth(true).setFlexGrow(2);
        resultsGrid.addColumn(h -> h.document().getCategory()).setHeader("CATEGORY").setAutoWidth(true);
        resultsGrid.addColumn(h -> String.format(Locale.ROOT,"%.2f",h.score())).setHeader("RELEVANCE").setAutoWidth(true);
        resultsGrid.addComponentColumn(hit -> { VerticalLayout v=new VerticalLayout(); v.setPadding(false); v.setSpacing(false); v.add(new Span(hit.snippet()==null?"":hit.snippet())); Button open=new Button("View",VaadinIcon.EXTERNAL_LINK.create(),e->showDocument(hit.document())); open.addClassName("quiet-button"); v.add(open); return v; }).setHeader("PREVIEW").setFlexGrow(3);
        resultsGrid.setVisible(false);
    }
    private void configureLibrary() {
        libraryGrid.addClassName("data-grid"); libraryGrid.setWidthFull(); libraryGrid.setAllRowsVisible(true);
        libraryGrid.addColumn(DocumentEntity::getTitle).setHeader("TITLE").setAutoWidth(true).setFlexGrow(2);
        libraryGrid.addColumn(DocumentEntity::getCategory).setHeader("CATEGORY").setAutoWidth(true);
        libraryGrid.addColumn(d -> d.getUpdatedAt().format(dateFormat)).setHeader("LAST UPDATED").setAutoWidth(true);
        libraryGrid.addComponentColumn(d -> { Button edit=new Button("Edit",VaadinIcon.EDIT.create(),e->openDocumentDialog(d)); edit.addClassName("quiet-button"); Button del=new Button(VaadinIcon.TRASH.create(),e->confirmDelete(d)); del.addClassName("danger-button"); del.getElement().setAttribute("aria-label","Delete document"); HorizontalLayout actions=new HorizontalLayout(edit,del); actions.setSpacing(true); return actions; }).setHeader("ACTIONS").setAutoWidth(true);
    }
    private void refreshLibrary() {
        List<DocumentEntity> all=documents.all(); libraryGrid.setItems(all); documentCount.setText(String.valueOf(all.size()));
        List<String> cats=new ArrayList<>(); cats.add("All categories"); cats.addAll(search.categories()); String selected=categoryFilter.getValue(); categoryFilter.setItems(cats); categoryFilter.setValue(selected!=null && cats.contains(selected)?selected:"All categories");
    }
    private void runSearch() {
        String q=query.getValue(); if(q==null||q.isBlank()){Notification.show("Enter a word or phrase to search.");return;}
        List<SearchHit> hits=search.search(q,categoryFilter.getValue(),100); resultsGrid.setItems(hits); resultsGrid.setVisible(true);
        resultSummary.removeAll(); resultSummary.add(new Span(hits.size()+" result"+(hits.size()==1?"":"s")+" for “"+q.trim()+"”"));
        if(hits.isEmpty()) resultSummary.add(new Span("No matches. Try fewer words or choose All categories."));
    }
    private void openDocumentDialog(DocumentEntity existing) {
        boolean editing=existing!=null; Dialog dialog=new Dialog(); dialog.setHeaderTitle(editing?"Edit document":"Add a document"); dialog.setWidth("min(760px, 95vw)");
        TextField title=new TextField("Title"); title.setWidthFull(); title.setRequiredIndicatorVisible(true);
        ComboBox<String> category=new ComboBox<>("Category"); category.setItems("Programming","Computer Science","Technology","Database","Research","Notes","Other"); category.setAllowCustomValue(true); category.addCustomValueSetListener(e->category.setValue(e.getDetail())); category.setWidthFull(); category.setRequiredIndicatorVisible(true);
        TextArea content=new TextArea("Document content"); content.setWidthFull(); content.setMinHeight("180px"); content.setPlaceholder("Paste text here, or upload a supported file below.");
        if(editing){title.setValue(existing.getTitle());category.setValue(existing.getCategory());content.setValue(existing.getContent());}
        MemoryBuffer buffer=new MemoryBuffer(); final String[] uploadedFilename = {null}; Upload upload=new Upload(buffer); upload.setMaxFiles(1); upload.setMaxFileSize(15*1024*1024); upload.setAcceptedFileTypes(".txt",".md",".csv",".pdf",".docx"); upload.setDropLabel(new Span("Drop TXT, MD, CSV, PDF or DOCX here"));
        Span fileStatus=new Span("Uploaded file text will fill the content field."); upload.addSucceededListener(event->{try{String extracted=extraction.extract(event.getFileName(), buffer.getInputStream().readAllBytes());uploadedFilename[0]=event.getFileName();content.setValue(extracted);if(title.isEmpty())title.setValue(stripExtension(event.getFileName()));fileStatus.setText("Text extracted from "+event.getFileName());}catch(Exception ex){Notification.show("Upload failed: "+ex.getMessage(),5000,Notification.Position.MIDDLE);}});
        Button save=new Button(editing?"Save changes":"Save document",VaadinIcon.CHECK.create(),e->{try{String filename=uploadedFilename[0];documents.save(title.getValue(),category.getValue(),content.getValue(),filename==null?(editing?existing.getOriginalFilename():null):filename,editing?existing.getId():null);dialog.close();refreshLibrary();Notification.show("Document saved and indexed.");}catch(Exception ex){Notification.show("Could not save: "+ex.getMessage(),5000,Notification.Position.MIDDLE);}});save.addClassName("primary-button");
        Button cancel=new Button("Cancel",e->dialog.close()); HorizontalLayout buttons=new HorizontalLayout(cancel,save); buttons.setJustifyContentMode(JustifyContentMode.END); VerticalLayout body=new VerticalLayout(new Paragraph("Add text directly or upload a file. The text is stored in the database and indexed for search."),title,category,upload,fileStatus,content,buttons); body.setPadding(true); body.setWidthFull(); dialog.add(body); dialog.open();
    }
    private void confirmDelete(DocumentEntity d) { Dialog dialog=new Dialog(); dialog.setHeaderTitle("Delete document?"); dialog.add(new Paragraph("“"+d.getTitle()+"” will be removed from the library and search index.")); Button cancel=new Button("Cancel",e->dialog.close()); Button delete=new Button("Delete",e->{documents.delete(d.getId());dialog.close();refreshLibrary();Notification.show("Document deleted.");}); delete.addClassName("danger-button"); dialog.getFooter().add(cancel,delete); dialog.open(); }
    private void showDocument(DocumentEntity d) { Dialog dialog=new Dialog(); dialog.setHeaderTitle(d.getTitle()); dialog.setWidth("min(800px, 95vw)"); Span cat=new Span("CATEGORY  /  "+d.getCategory()); cat.addClassName("category-pill"); TextArea body=new TextArea("Full content");body.setWidthFull();body.setReadOnly(true);body.setValue(d.getContent());body.setMinHeight("300px"); Button close=new Button("Close",e->dialog.close()); Button download=new Button("Download text",VaadinIcon.DOWNLOAD.create(),e->{StreamResource resource=new StreamResource(fileSafe(d.getTitle())+".txt",()->new ByteArrayInputStream(d.getContent().getBytes(StandardCharsets.UTF_8))); UI.getCurrent().getPage().open(resource, "_blank");}); HorizontalLayout buttons=new HorizontalLayout(download,close); dialog.add(cat,body,buttons); dialog.open(); }
    private static String stripExtension(String n){int i=n.lastIndexOf('.');return i>0?n.substring(0,i):n;}
    private static String fileSafe(String s){return s.replaceAll("[^A-Za-z0-9._-]","_");}
}
