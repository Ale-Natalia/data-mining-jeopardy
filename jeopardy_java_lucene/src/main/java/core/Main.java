package core;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Main {

    private static void addDoc(IndexWriter w, String title, String content) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        w.addDocument(doc);
    }

    public static List<Question> readQuestions(PreProcessor preProcessor, String filePath) throws IOException {
        System.out.println("[Reading the questions]");
        List<Question> questions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String category = line.trim();
                line = br.readLine();
                String clue = line.trim();
                line = br.readLine();
                String answer = line.trim();
                br.readLine();

                clue = preProcessor.preprocessText(clue);

                Question question = new Question(category, clue, answer);
                questions.add(question);
            }
        }

        return questions;
    }

    public static Map<Integer, CustomDocument> readDocuments(PreProcessor preProcessor, IndexWriter w) {
        System.out.println("[Reading the documents and creating the index]");
        long startTime = System.currentTimeMillis();

        Map<Integer, CustomDocument> documents = new ConcurrentHashMap<>();

        final Integer[] docIndex = {0};
        final Integer[] fileIndex = {0};
        File folder = new File("src/main/resources/wikipages");
        Arrays.stream(folder.listFiles()).parallel().forEach(fileEntry -> {
            synchronized (fileIndex[0]) {
                System.out.println(fileIndex[0]);
                fileIndex[0]++;
            }
            if (fileEntry.isFile()) {
                BufferedReader br;
                try {
                    br = new BufferedReader(new FileReader(fileEntry.getPath()));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                String line;
                String documentTitle = "";
                String documentContent = "";
                while (true) {
                    try {
                        if ((line = br.readLine()) == null) break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (line.startsWith("[[") && line.endsWith("]]")) {
                        if (!documentTitle.equals("")) {

                            documentTitle = documentTitle.substring(2, documentTitle.length() - 2);
                            documents.put(docIndex[0], new CustomDocument(docIndex[0], documentTitle, documentContent));

                            documentTitle = preProcessor.preprocessText(documentTitle);
                            documentContent = preProcessor.preprocessText(documentContent);

                            documentContent = documentTitle + " " + documentContent;

                            try {
                                addDoc(w, documentTitle, documentContent);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            documentContent = "";
                            synchronized (docIndex[0]) {
                                docIndex[0]++;
                            }
                        }
                        documentTitle = line;
                    }
                    documentContent += line;
                }
            }
        });

        long endTime = System.currentTimeMillis();
        System.out.println("Time taken to build index: " + (endTime - startTime) + " milliseconds.");
        return documents;
    }

    private static boolean indexExistsInResources(String indexPath) {
        ClassLoader classLoader = Main.class.getClassLoader();
        URL resource = classLoader.getResource(indexPath);

        if (resource != null) {
            try {
                Path indexPathInFileSystem = Paths.get(resource.toURI());
                return Files.exists(indexPathInFileSystem) && Files.isDirectory(indexPathInFileSystem);
            } catch (Exception e) {
                e.printStackTrace(); // Handle exception as needed
            }
        }

        return false;
    }

    private static List<CustomDocument> searchDocuments(String queryString, int maxResults, QueryParser queryParser, IndexSearcher searcher) throws Exception {
        List<CustomDocument> documents = new ArrayList<>();
        queryString = PreProcessor.lemmatize(queryString);
        Query query = queryParser.parse(queryString);
        ScoreDoc[] hits = searcher.search(query, maxResults).scoreDocs;

        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document luceneDocument = searcher.doc(docId);

            String documentTitle = luceneDocument.get("title");
            String documentContent = luceneDocument.get("content");

            documents.add(new CustomDocument(docId, documentTitle, documentContent));
        }

        return documents;
    }


    public static void main(String[] args) throws Exception {

        PreProcessor preProcessor = new PreProcessor();

        List<Question> questions = readQuestions(preProcessor, "src/main/resources/questions.txt");

        Analyzer analyzer = new EnglishAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        String indexPath = "src/main/resources/index_lemma";
        String indexDirName = "index_lemma";

        Path indexDirPath = Paths.get(indexPath);

        Directory indexDirectory = FSDirectory.open(indexDirPath);

        Map<Integer, CustomDocument> documents;

        if (indexExistsInResources(indexDirName)) {
            IndexReader indexReader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            QueryParser queryParser = new QueryParser("content", analyzer);

            int maxResults = 15;
            double sum = 0;
            int numberAnswersTop3 = 0;
            int numberAnswersTop1 = 0;

            for (Question question : questions) {
                String query = question.category + "\n" + question.clue;
                String answer1;
                String answer2 = "";
                boolean flag = false;
                if (question.answer.contains("|")) {
                    flag = true;
                    answer1 = PreProcessor.lemmatize(question.answer.split("\\|")[0]);
                    answer2 = PreProcessor.lemmatize(question.answer.split("\\|")[1]);
                } else {
                    answer1 = PreProcessor.lemmatize(question.answer);
                    answer1 = answer1.replace(" - ", "");
                    answer1 = answer1.replace("'", "");
                }
                System.out.println("\n\n" + question.clue + " " + question.category + "\nExpected answer: " + question.answer);
                System.out.println("ANSWERS\n");

                List<CustomDocument> result = searchDocuments(query, maxResults, queryParser, searcher);

                int i = 1;
                int res = -1;
                for (CustomDocument document : result) {
                    System.out.println("Document ID: " + document.getId());
                    System.out.println("Title: " + document.getTitle());

                    if (flag) {
                        if (document.getTitle().equalsIgnoreCase(answer1) || document.getTitle().equalsIgnoreCase(answer2)) {
                            res = i;
                            break;
                        }
                    } else {
                        if (document.getTitle().equalsIgnoreCase(answer1)) {
                            res = i;
                            break;
                        }
                    }
                    i++;
                    System.out.println("---");
                }
                if (res != -1) {
                    sum += (double) 1 / res;
                    if (res <= 3)
                        numberAnswersTop3++;
                    if (res == 1)
                        numberAnswersTop1++;
                }
            }
            System.out.println(sum * 100 / questions.size());
            System.out.println(numberAnswersTop3 * 100 / questions.size());
            System.out.println(numberAnswersTop1 * 100 / questions.size());

        } else {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(indexDirectory, config);
            documents = readDocuments(preProcessor, w);
            w.close();
            System.out.println("Number of documents: " + documents.size());

        }
        System.out.println("[Done]");
    }
}
