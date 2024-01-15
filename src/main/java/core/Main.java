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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


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
                // Read triplet
                String category = line.trim();
                line = br.readLine();
                String clue = line.trim();
                line = br.readLine();
                String answer = line.trim();
                br.readLine(); // Skip new line

                // Pre-process clue text
                clue = preProcessor.preprocessText(clue);

                // Create question (category, clue, and answer)
                Question question = new Question(category, clue, answer);
                questions.add(question);
            }
        }

        return questions;
    }

    public static Hashtable<Integer, CustomDocument> readDocuments(PreProcessor preProcessor, IndexWriter w, String resourceName) throws IOException {
        System.out.println("[Reading the documents and creating the index]");
        long startTime = System.currentTimeMillis();

        Hashtable<Integer, CustomDocument> documents = new Hashtable<>();

        int docIndex = 0;
        int fileIndex = 0;
        File folder = new File("src/main/resources/wikipages");
        // TODO: parallelize this for loop
        for (final File fileEntry : folder.listFiles()) {
            System.out.println(fileIndex);
            fileIndex++;
            if (fileEntry.isFile()) {
                // Read the content of each file
                BufferedReader br = new BufferedReader(new FileReader(fileEntry.getPath()));
                String line;
                String documentTitle = "";
                String documentContent = "";
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("[[") && line.endsWith("]]")) {
                        if (!documentTitle.equals("")) {

                            // Remove trailing square brackets
                            documentTitle = documentTitle.substring(2, documentTitle.length() - 2);

                            // Store raw data
//                            System.out.println(documentContent.length());
                            documents.put(docIndex, new CustomDocument(docIndex, documentTitle, documentContent));

                            // Pre-process texts
                            documentTitle = preProcessor.preprocessText(documentTitle);
                            documentContent = preProcessor.preprocessText(documentContent);

                            documentContent = documentTitle + " " + documentContent;

                            // Add document to index
                            addDoc(w, documentTitle, documentContent);

                            documentContent = "";
                            docIndex++;
                        }
                        documentTitle = line;
                    }
                    documentContent += line;
                }
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Time taken to build index: " + (endTime - startTime) + " milliseconds.");
        return documents;
    }

    public static Hashtable<Integer, ArrayList<Integer>> getGroundTruthMap(List<Question> questions, Hashtable<Integer, CustomDocument> documents) {
        System.out.println("[Computing the groundtruth for each question]");

        // m * n time complexity
        Hashtable<Integer, ArrayList<Integer>> groundTruth = new Hashtable<>();

        for (int i = 0; i < questions.size(); i ++) {
            // Add empty array for question index
            groundTruth.put(i, new ArrayList<>());

            // Iterate over all answers (document titles)
            for (Integer key : documents.keySet()) {
                // If answer has multiple correct choices, compare against each
                String[] correctAnswers = questions.get(i).answer.split("\\|");
                for (String correctAnswer : correctAnswers) {
                    if (documents.get(key).getTitle().equals(correctAnswer)) {
                        groundTruth.get(i).add(documents.get(key).getId());
                    }
                }
            }
        }
        return groundTruth;
    }

    // TODO: check metrics implementations - results are sometimes weird
    public static double computeMAPBetter(HashMap<Integer, ScoreDoc[]> scores, List<Question> questions, Hashtable<Integer,
            ArrayList<Integer>> groundTruth, int maxK) {
        double mapScore = 0;
        int docsWithGroundTruth = 0;

        for (int i=0; i<questions.size(); i++) {
            if (groundTruth.get(i).size() > 0) {
                docsWithGroundTruth++;

                mapScore += computeAveragePrecisionBetter(scores.get(i), groundTruth.get(i), maxK);
            }
        }
        return mapScore / docsWithGroundTruth * 100;
    }

    // TODO: check metrics implementations - results are sometimes weird
    public static double computeAveragePrecisionBetter(ScoreDoc[] retrievedDocs, ArrayList<Integer> groundTruthDocuments, int maxK) {
        // Get array of correct/incorrect predictions
        ArrayList<Integer> predictionsLabeled = new ArrayList<>();
        for(int i=0; i<retrievedDocs.length; i++) {
            if (groundTruthDocuments.contains(retrievedDocs[i].doc)) {
                predictionsLabeled.add(1);
            } else{
                predictionsLabeled.add(0);
            }
        }

        // There is only one ground-truth, so always divide by 1 - no division added
        return computePrecision(predictionsLabeled, 1, maxK);
    }

    // TODO: check metrics implementations - results are sometimes weird
    public static double computePrecision(ArrayList<Integer> predictionsLabeled, int j, int maxK) {
        // j = until which gt value to search - always 1 in our problem
        // maxK = how many predictions to consider at most
        int countFoundGTs = 0;
        int i = 0;
        double precision = 0.0;
//        while (i < predictionsLabeled.size() && countFoundGTs < j && i < maxK) {
        // TODO - try without i < predictionsLabeled.size() - don't stop
        while (i < maxK && i < predictionsLabeled.size() && countFoundGTs < j) {
            if (predictionsLabeled.get(i) == 1) {
                countFoundGTs++;
                precision += (double) countFoundGTs / (i + 1);
            }
            i++;
        }
        return precision;
    }

    // TODO: (LOW PRIORITY) maybe compute other metrics too

    // TODO: (later) maybe try other config for building index:
    // currently we are considering all words in a file in the same way; maybe we could treat titles, subtitles
    // differently, connect somehow each section to its chapter title etc.

    public static void main(String[] args) throws IOException, ParseException {

        // Initialize data preprocessor
        PreProcessor preProcessor = new PreProcessor();

        // Read questions
        List<Question> questions = readQuestions(preProcessor, "src/main/resources/questions.txt");

        // Create an analyzer
        Analyzer analyzer = new EnglishAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
//        Analyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        // Create an index
        String indexPath = "src/main/resources/newfile";

        // Convert the string path to a Path object
        Path indexDirPath = Paths.get(indexPath);

        // TODO: Write index if not already written,
        // else read it from /newfile

        // Open the FSDirectory using the Path object
        Directory index = FSDirectory.open(indexDirPath);


        // Create a default configuration for index writing
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        // Create an index writer
        IndexWriter w = new IndexWriter(index, config);
        // Read documents and write them to index
        Hashtable<Integer, CustomDocument> documents = readDocuments(preProcessor, w, "wikipages");
        w.close();

        System.out.println("Number of documents: " + documents.size() + "; dbg " + documents.get(1000).getTitle());

        // Create ground-truth
        Hashtable<Integer, ArrayList<Integer>> groundTruth = getGroundTruthMap(questions, documents);

        // Create an index searcher that searches through the documents
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);

        // For each question, find top 5 relevant documents
        int hitsPerPage = 15;
        HashMap<Integer, ScoreDoc[]> scores = new HashMap<>();
        int countGroundTruthNotFound = 0;
        for (int idx=0; idx<questions.size(); idx++) {

            if (groundTruth.get(idx).size() == 0) {
                // No ground truth found for this question
                countGroundTruthNotFound++;
                continue;
            }

            Question question = questions.get(idx);

            // TODO: (later) try different ways of interpreting the question -> probably from parser config & methods
            // maybe we can consider question title and categories differently -> I think we are currently just treating
            // all the words in the question the same

            // Create a query parser
            String queryToParse = question.category + " " + question.clue; // category + clue
//            String queryToParse = question.clue; // only clue
            Query q = new QueryParser("content", analyzer).parse(queryToParse);// Search the documents that match the query
            TopDocs docs = searcher.search(q, hitsPerPage); // Store hits inside scores dictionary
            ScoreDoc[] hits = docs.scoreDocs;
            scores.put(idx, hits);

            // TODO: save all results to file or copy them from the terminal
            System.out.println("Question: " + question.clue + "; Expected answer: " + question.answer);
            System.out.println("Found " + hits.length + " hits.");
            for (int i = 0; i < hits.length; i++) {
                int docId = hits[i].doc;
                /*Document d = searcher.storedFields().document(docId);*/
                if (documents.get(docId).getTitle() != null) {
                    System.out.println((i + 1) + ". " + documents.get(docId).getTitle());
                }
            }
            System.out.println("");
            System.out.println("");
        }

        // Evaluate the system for different k values
        // TODO: save all metrics to file or copy them from the terminal
        for (int k=1; k <= 15; k ++) {

            double mapb = computeMAPBetter(scores, questions, groundTruth, k);
            System.out.println("MAP score better k=" + k + ": " + mapb + "%");
        }

//        System.out.println("Did not consider " + countGroundTruthNotFound + "/" + questions.size() + " questions.");
        System.out.println("[Done]");
    }
}
