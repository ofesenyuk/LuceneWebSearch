/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spd.ukraine.lucenewebsearch1.web;

import com.spd.ukraine.lucenewebsearch1.model.WebPage;
import com.spd.ukraine.lucenewebsearch1.service.WebPagesService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.validation.Valid;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author sf
 */

@Controller
public class IndexingController {
    private static final int MAX_HITS = 100;
    private static final String URL_FIELD = "url";
    private static final String TITLE_FIELD = "title";
    private static final String CONTENT_FIELD = "content";
    private static final String HREF = "href";
    private static final String SORTING_BY_RELEVANCE = "Sorting By Relevance";
    private static final String SORTING_ALPHABETICALLY 
            = "Sorting Alphabetically";
    private static final boolean IS_DIRECTORY_IN_DISK = false;
    private static final int MAX_NUMBER_SITES_INDEXED = 300;
    private static final int RESULTS_PER_PAGE = 10;
    private static final String HIGHLIGHT_OPEN 
            = "<b><span style='background-color:yellow'>";
    private static final String HIGHLIGHT_CLOSE 
            = "</span></b>";
    private static final int MAX_WINDOW_SIZE = 500;
    
    private Integer MAX_RECURSION_SEARCH_NUMBER = 2; //static final
    private int lastPageResultIndex = RESULTS_PER_PAGE;
    private String sortingOrder = SORTING_BY_RELEVANCE;
    
    private File indexDir = null;
    private Directory directory = null;
    private StandardAnalyzer analyzer = null;
    private IndexWriter indexWriter = null;
    
    private final Set<String> referencedSites = new HashSet<>();
    private final Set<String> referencedTitles = new HashSet<>();
    List<WebPage> foundSearchResults = new ArrayList<>();
    List<WebPage> foundSearchResultsSorted = new ArrayList<>();
    
    
//    @Autowired
//    private WebPagesService webPagesService;
    
    @PostConstruct
    public void init() {
        if (IS_DIRECTORY_IN_DISK) {
            String userDirectory = System.getProperty("user.dir");// + "/lucene"; 
            System.out.println("userDirectory " + userDirectory);
            Path userPath = Paths.get(userDirectory);
            Path rootPath = userPath.getRoot();
            String workingDirectory = rootPath.toString()
                    .concat(System.getProperty("file.separator").equals("/")
                            ? userPath.subpath(0, 2).toString() + "/"
                            : "\\Users\\sf\\")
                    .concat("luceneindex");
            System.out.println("workingDirectory " + workingDirectory);
            indexDir = new File(workingDirectory);
            try {
                Files.createDirectory(Paths.get(workingDirectory));
            } catch (FileAlreadyExistsException ex) {
                System.out.println("FileAlreadyExistsException");
            } catch (IOException ex) {
//            System.out.println("IOException: " + ex.getMessage());
                ex.printStackTrace();
            }
            if (null == indexDir) {
                return;
            }
            try {
                directory = FSDirectory.open(indexDir);
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        } else {
            directory = new RAMDirectory();
        }
        analyzer = new StandardAnalyzer(Version.LUCENE_43);//new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43,
                analyzer);
        try {
            indexWriter = new IndexWriter(directory, config);
        } catch (IOException ex) {
//            ex.printStackTrace();
//            return;
        }
    }
    
    /**
     * Method used to create indexing for web page url to be entered.
     *
     * @return model of view
     */
    @RequestMapping(value = {"/index", "/"},
            method = RequestMethod.GET)
    public ModelAndView welcomePage() {
        System.out.println("welcomePage");
        ModelAndView model = new ModelAndView();
        WebPage q = new WebPage();
        q.setTitle(MAX_RECURSION_SEARCH_NUMBER.toString());
        model.addObject("q", q);
        model.setViewName("index");
        return model;
    }
    
    /**
     * Method used to create indexing for entered web page content and web 
     * pages referenced from given web page.
     *
     * @param webPage webPage.url == entered web-page url
     * @param result used to detect errors in form
     * @param request for future code
     * @param errors for future code
     * @return model of the success view or that of the create indexing page
     */
    @RequestMapping(value = "/indexing",
            method = RequestMethod.POST)
    public ModelAndView startIndexing(@ModelAttribute("q")
            @Valid WebPage webPage, BindingResult result, WebRequest request,
            Errors errors) {
        System.out.println("start indexing q = " + webPage.getUrl());
        int maxRecursion = MAX_RECURSION_SEARCH_NUMBER;
        try {
            maxRecursion = Integer.parseInt(webPage.getTitle());
        } catch (NumberFormatException e) {
            
        }
        MAX_RECURSION_SEARCH_NUMBER = Math.abs(maxRecursion);
        WebPage created = new WebPage();
        if (!result.hasErrors()) {
            System.out.println("!result.hasErrors()");
            created = createWebPageRecord(webPage);
        }
        if (created == null) {
            System.out.println("created == null");
            result.rejectValue("url", "label.not.reached.address");
        }
        if (result.hasErrors()) {
            System.out.println("result.hasErrors()");
            ModelAndView model = new ModelAndView("index");
            model.addObject("q", webPage);
            return model;
        } else {
            ModelAndView model = new ModelAndView("root");
            model.addObject("q", new WebPage());
            return model;
        }
    }
    
    /**
     * Method used to initiate search of given keywords in data base.
     *
     * @param keywords keywords
     * @param result used to detect errors in form
     * @param request for future code
     * @param errors for future code
     * @return model of the success view or that of the registration page
     */
    @RequestMapping(value = {"/search"}) //,method = RequestMethod.POST)
    public ModelAndView search(@ModelAttribute("q")
            WebPage keywords, BindingResult result, WebRequest request,
            Errors errors) {
        System.out.println("search for keywords q = " + keywords.getTitle());
        foundSearchResults.clear();
        foundSearchResultsSorted.clear();
        if (!result.hasErrors()) {
            System.out.println("!result.hasErrors()");
            try {
                foundSearchResults 
                        = new ArrayList<>(searchPhrase(keywords.getTitle(), 
                                CONTENT_FIELD));
            } catch (IOException | ParseException ex) {
                Logger.getLogger(IndexingController.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        if (foundSearchResults.isEmpty()) {
            System.out.println("found.isEmpty()");
            result.rejectValue("title", "label.not.found");
        }
        if (result.hasErrors()) {
            System.out.println("result.hasErrors()");
            ModelAndView model = new ModelAndView("root");
            model.addObject("q", keywords);
            return model;
        } else {
            lastPageResultIndex = RESULTS_PER_PAGE;
            ModelAndView model = new ModelAndView("results");
            model.addObject("found", returnSubListOfSortedList()); //foundSearchResults.subList(0, lastPageResultIndex));
            model.addObject("sorting", returnInvertedSortingOrder());
            return model;
        }
    }
    
    /**
     * Method used to redirect to page with next (RESULTS_PER_PAGE) results.
     *
     * @return model with next results 
     */
    @RequestMapping(value = "next")
    public ModelAndView nextResultsPage() {
        lastPageResultIndex += RESULTS_PER_PAGE;
        lastPageResultIndex = lastPageResultIndex <= foundSearchResults.size()
                ? lastPageResultIndex : foundSearchResults.size();
        System.out.println("next " + lastPageResultIndex);
        ModelAndView model = new ModelAndView();
        WebPage q = new WebPage();
        q.setTitle(MAX_RECURSION_SEARCH_NUMBER.toString());
        model.addObject("found", returnSubListOfSortedList());
        model.addObject("sorting", returnInvertedSortingOrder());
        model.setViewName("results");
        return model;
    }

        /**
     * Method used to provide view with the selected sorting.
     *
     * changes sortingOrder, lastPageResultIndex, and foundSearchResultsSorted
     * @return model of the first page with new selected sorting
     */
    @RequestMapping(value = "sorting")
    public ModelAndView sorting() {
        sortingOrder = returnInvertedSortingOrder();
//        lastPageResultIndex = lastPageResultIndex <= foundSearchResults.size()
//                ? lastPageResultIndex : foundSearchResults.size();
        lastPageResultIndex = RESULTS_PER_PAGE;
        System.out.println("next " + lastPageResultIndex);
        WebPage q = new WebPage();
        q.setTitle(MAX_RECURSION_SEARCH_NUMBER.toString());
        ModelAndView model = new ModelAndView();
        model.addObject("sorting", returnInvertedSortingOrder());
        model.addObject("found", returnSubListOfSortedList());
        model.setViewName("results");
        return model;
    }

    /**
     * Method used to create the record of given web page in search database.
     *
     * @param webPage webPage.url is entered url
     * @return webPage for success or null for fail
     */
    private WebPage createWebPageRecord(WebPage webPage) {
        try {
            Document html = Jsoup.connect(webPage.getUrl()).get();
            referencedSites.clear();
            indexElements(webPage, html, 0);
            System.out.println(html.text());
            System.out.println("number of indexed fields is " 
                    + indexWriter.numDocs());
//            indexWriter.commit();
            indexWriter.close();
            return webPage;
        } catch (Exception  ex) {
            System.out.println("createWebPageRecord " + ex.getMessage());
//            ex.printStackTrace();
            return null;
        } 
    }

    /**
     * Method used to perform recursive creation indexing for a given web page 
     * in search database.
     *
     * @param webPage webPage.url is entered url
     * webPage.title is set
     * @param html Jsoup.Document of entered url
     * @param recursionNumber used to stop recursion at exceeding 
     * MAX_RECURSION_SEARCH_NUMBER
     */
    private void indexElements(WebPage webPage, Document html, 
            final int recursionNumber) throws IOException, ParseException { 
        String title = html.title();
        if (referencedTitles.contains(title.trim())) {
            return;
        }
        referencedTitles.add(title.trim());
        webPage.setTitle(title);
        if (containsPage(webPage)) {
            System.out.println(webPage.getUrl() + " is already indexed");
            return;
        }
        Element prevElement = null;
        Elements elements = html.body().getAllElements(); //.getElementsByTag("a");
        addDoc(webPage, html.text());
//        for (Element element : elements) {
////                System.out.println(element.nodeName() + " element.text() " 
////                        + element.text() + " url " 
////                        + element.absUrl("href"));
//            if (element.nodeName().equalsIgnoreCase("body")) {
//                addDoc(webPage, element.text());
//                break;
////                continue;
//            }
//            if (null == prevElement) {
//                prevElement = element;
////            } else if (prevElementContainsElementText(prevElement, element)) {
////                continue;
//            }
////            if (null !== webPagesService.findWebPage(element.absUrl("href")))
//            if (element.text().trim().isEmpty()) {
//                continue;
//            }
////            StringTokenizer str = new StringTokenizer(element.text());
////            str.
//            addDoc(webPage, element.text());
//        }
        if (recursionNumber > MAX_RECURSION_SEARCH_NUMBER
                || referencedSites.size() > MAX_NUMBER_SITES_INDEXED) {
//            System.out.println(recursionNumber + " " 
//                    + referencedSites.contains(webPage.getUrl()));
            return;
        }
        elements.parallelStream()
                .filter((Element e) -> e.nodeName().equalsIgnoreCase("a") 
                                       && null != e.absUrl(HREF)
                                       && !e.absUrl(HREF).trim().isEmpty()
                                       && !referencedSites
                                               .contains(e.absUrl(HREF))
                                       && !referencedSites
                                               .contains(removeSharpEtc(e
                                                       .absUrl(HREF))))
                .forEach((Element element) -> {
                    WebPage webPage1 = new WebPage(element.absUrl(HREF));
                    String url1 = webPage1.getUrl(); 
//                    System.out.println(recursionNumber + " recursion for '" 
//                            + url1 + "'");
                    try {
                        Document htmlR = Jsoup.connect(url1).get();
                        indexElements(webPage1, htmlR, recursionNumber + 1);
                    } catch (IOException | ParseException e ) {
                        System.out.println("Exception " + e.getMessage());
                    }
                    referencedSites.add(url1);
                });
//        for (Element element : elements) {
//            if (!element.nodeName().equalsIgnoreCase("a")) {
//                continue;
//            }
//            WebPage webPage1 = new WebPage(element.absUrl("href"));
//            if (null == webPage1.getUrl() 
//                    || webPage1.getUrl().isEmpty()
//                    || referencedSites.contains(webPage1.getUrl())) {
//                continue;
//            }
//            System.out.println(recursionNumber + "recursion for " 
//                    + element.absUrl("href"));
//            try {
//                Document htmlR = Jsoup.connect(webPage1.getUrl()).get();
//                webPage1.setTitle(htmlR.title());
//                indexElements(webPage1, htmlR, recursionNumber + 1);
//            } catch (IOException e) {
//                System.out.println("IOException " + e.getMessage());
//            }
//            referencedSites.add(webPage1.getUrl());
//        }
    }

    private boolean prevElementContainsElementText(Element prevElement, 
            Element element) {
        return (prevElement.hasText() && element.hasText() 
                && prevElement.text().contains(element.text()));
    }

    /**
     * Method used to add documents to search database .
     *
     * @param webPage webPage.url is entered url; webPage.title is also added
     * @param text is also added
     */
    private void addDoc(WebPage webPage, String text) throws IOException {
        org.apache.lucene.document.Document doc 
                = new org.apache.lucene.document.Document();
        doc.add(new TextField(URL_FIELD, QueryParser.escape(webPage.getUrl()), 
                Field.Store.YES));
        doc.add(new TextField(TITLE_FIELD, webPage.getTitle(), 
                Field.Store.YES));
        doc.add(new TextField(CONTENT_FIELD, text, Field.Store.YES));
        System.out.println("text '" + text + "'");
        System.out.println("addDocument(doc) " + doc.get(URL_FIELD));  
        if (null == indexWriter) {
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, 
                    analyzer);
            System.out.println("config " + config);
            indexWriter = new IndexWriter(directory, config);
        }
//        System.out.println("indexWriter " + indexWriter);
        indexWriter.addDocument(doc);
    }
    
    public boolean containsPage(String url)
            throws IOException, ParseException {

//        searchIndex(indexDir, url, hits);
//        Directory directory = FSDirectory.open(indexDir);//.getDirectory(indexDir);
        try {
            System.out.println("directory.listAll() " 
                    + Arrays.toString(directory.listAll()));
            IndexReader indexReader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            QueryParser parser = new QueryParser(Version.LUCENE_43, URL_FIELD, 
                    new StandardAnalyzer(Version.LUCENE_43)); //new SimpleAnalyzer()
            org.apache.lucene.search.Query query 
                    = parser.parse(QueryParser.escape(url));
            TopDocs topDocs = searcher.search(query, 1);
            ScoreDoc[] hits = topDocs.scoreDocs;
            System.out.println("hits.length " + hits.length);
            return hits.length > 0;
        } catch (org.apache.lucene.index.IndexNotFoundException ex) {
            return false;
        }
    }
    
    private boolean containsPage(WebPage webPage) {
        try {
            return !searchPhrase(webPage.getTitle(), TITLE_FIELD).isEmpty();
        } catch (IOException | ParseException ex) {
            return false;
        }
    }
    
    /**
     * Method used to search phrase in search database.
     *
     * @param phrase
     * @param fieldName to search in it
     * @return Collection<WebPage> with search data saved in WebPage fields
     * @throws java.io.IOException
     * @throws org.apache.lucene.queryparser.classic.ParseException
     */
    public Collection<WebPage> searchPhrase(String phrase, String fieldName) 
                throws IOException, ParseException {
        if (null == phrase) {
            return new ArrayList<>();
        }
//        searchIndex(indexDir, phrase, hits);
//        Directory directory = FSDirectory.open(indexDir);//.getDirectory(indexDir);
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        QueryParser parser = new QueryParser(Version.LUCENE_43, fieldName, 
                new StandardAnalyzer(Version.LUCENE_43)); //new SimpleAnalyzer()
        org.apache.lucene.search.Query query = parser.parse(phrase);
        TopDocs topDocs = searcher.search(query, MAX_HITS);
        ScoreDoc[] hits = topDocs.scoreDocs;
        List<WebPage> searchResults = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            org.apache.lucene.document.Document d = searcher.doc(docId);
            System.out.println("'" + d.get(URL_FIELD) + "' '" 
                    + d.get(TITLE_FIELD) + "'"); // + " " + d.get("content"));
            WebPage webPage = new WebPage();
            webPage.setUrl(d.get(URL_FIELD));
            webPage.setTitle(highLightPhrase(d.get(TITLE_FIELD), phrase));
            webPage.setContent(truncateText(d.get(CONTENT_FIELD), phrase)); 
            searchResults.add(webPage);
        }
        System.out.println("Found " + hits.length);
        return new LinkedHashSet<>(searchResults);
    }

    private Object returnSubListOfSortedList() {
        List<WebPage> sortedList; 
        if (sortingOrder.equals(SORTING_ALPHABETICALLY)) {
            if (foundSearchResultsSorted.isEmpty()) {
                foundSearchResultsSorted = new ArrayList<>(foundSearchResults);                
                Collections.sort(foundSearchResultsSorted, 
                        (o1, o2) -> {
                            String title1 = o1.getTitle()
                                    .replace(HIGHLIGHT_OPEN, "");
                            String title2 = o2.getTitle()
                                    .replace(HIGHLIGHT_OPEN, "");
                            return title1.compareToIgnoreCase(title2);});
            }
            sortedList = foundSearchResultsSorted;
//            System.out.println("sortedList '" + sortedList.get(0).getTitle() 
//                + "', '" + sortedList.get(1).getTitle());
        } else {
            sortedList = foundSearchResults;
        }
        return sortedList.subList(lastPageResultIndex - RESULTS_PER_PAGE, 
                Math.min(lastPageResultIndex, sortedList.size()));
    }

    private String returnInvertedSortingOrder() {
        return sortingOrder.equals(SORTING_ALPHABETICALLY) 
                ? SORTING_BY_RELEVANCE : SORTING_ALPHABETICALLY;
    }

    private String highLightPhrase(String text, String phrase) {
        String lowerText = text.toLowerCase();
        String lowerPhrase = phrase.toLowerCase();
        StringBuilder newText = new StringBuilder(text);
        for (int i = lowerText.indexOf(lowerPhrase); 
                i >= 0; 
                i = lowerText.indexOf(lowerPhrase, i + HIGHLIGHT_OPEN.length() 
                        + phrase.length() + HIGHLIGHT_CLOSE.length() + 1)) {
            newText.insert(i, HIGHLIGHT_OPEN)
                    .insert(i + HIGHLIGHT_OPEN.length() + phrase.length(), 
                            HIGHLIGHT_CLOSE);
            lowerText = newText.toString().toLowerCase();
        }
        return newText.toString();
    }

    private String removeSharpEtc(String absUrl) {
        absUrl = absUrl.trim();
        if (absUrl.contains("#")) {
            absUrl = absUrl.split("#")[0];
        }
        if (absUrl.endsWith("index.html")) {
            absUrl = absUrl.replace("index.html", "");
        }
        if (absUrl.startsWith("http")) {
            if (absUrl.startsWith("https")) {
                absUrl = absUrl.replaceFirst("https", "http");
            } else {
                absUrl = absUrl.replaceFirst("http", "https");
            }
        }
        return absUrl;
    }

    private String truncateText(String text, String phrase) {
        StringBuilder inputPhrase = new StringBuilder();
//        try {
//            URL url = new URL(text);
//            BufferedReader in = new BufferedReader(new InputStreamReader(url
//                    .openStream()));
//            String inputLine;
//            while ((inputLine = in.readLine()) != null) {
//                inputPhrase.append(inputLine);
//                if (inputPhrase.toString().contains(phrase)
//                        && inputPhrase.length() > MAX_WINDOW_SIZE) {
//                    break;
//                }
//            }
//            in.close();
//        } catch (IOException ex) {
//            return "";
//        }
        inputPhrase = new StringBuilder(text);
        String lowerText = text.toLowerCase();
        int pos = lowerText.indexOf(phrase);
        try {
        inputPhrase.delete(0, 
                Math.max(0, pos - MAX_WINDOW_SIZE))
                .delete(Math.min(pos + MAX_WINDOW_SIZE, inputPhrase.length()), 
                        inputPhrase.length());
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("pos - MAX_WINDOW_SIZE " 
                    + (pos - MAX_WINDOW_SIZE));
            System.out.println("pos + MAX_WINDOW_SIZE " 
                    + (pos + MAX_WINDOW_SIZE) + " < " + inputPhrase.length());
            System.out.println(e.getMessage() + " text " + text);            
        }
        return highLightPhrase(inputPhrase.toString(), phrase);
    }

}
