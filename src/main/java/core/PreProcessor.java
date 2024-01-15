package core;

import edu.stanford.nlp.simple.Sentence;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class PreProcessor {

    InputStream tokenModelIn;
    TokenizerModel tokenModel;
    Tokenizer tokenizer;

    InputStream posModelIn;
    POSModel posModel;
    POSTagger posTagger;

    InputStream lemmaModelIn;
//    LemmatizerModel lemmaModel;
//    Lemmatizer lemmatizer;
    LemmatizerModel lemmatizer;
    DictionaryLemmatizer dictionaryLemmatizer;

//    Analyzer analyzer;

    public PreProcessor() throws IOException {

        ClassLoader classLoader = PreProcessor.class.getClassLoader();

        // NLP models initialisation
        /*
        this.tokenModelIn = new FileInputStream(classLoader.getResource("en-token.bin").getPath());
        this.tokenModel = new TokenizerModel(tokenModelIn);
        this.tokenizer = new TokenizerME(tokenModel);

        this.posModelIn = new FileInputStream(classLoader.getResource("en-pos-maxent.bin").getPath());
        this.posModel = new POSModel(posModelIn);
        this.posTagger = new POSTaggerME(posModel);

        this.lemmaModelIn = new FileInputStream(classLoader.getResource("en-lemmatizer.dict").getPath());

         */
//        this.lemmatizer = new LemmatizerModel(lemmaModelIn);
//        this.lemmatizer = new LemmatizerME(lemmaModel);
        //this.dictionaryLemmatizer = new DictionaryLemmatizer(lemmaModelIn);

//        this.analyzer = new EnglishAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
//        this.analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    }

    private String lemmatize(String content) {
        // Tokenize, POS Tag, and Lemmatize
        /*
        String[] tokens = this.tokenizer.tokenize(content);
        String[] tags = this.posTagger.tag(tokens);
        String[] lemmas = this.dictionaryLemmatizer.lemmatize(tokens, tags);
        return String.join(" ", lemmas);

         */

        Sentence sentence = new Sentence(content);

        List<String> lemmas = sentence.lemmas();

        return String.join(" ", lemmas);
    }

    public String preprocessText(String content) {

        // Lowercase
        content = content.toLowerCase();

        // Remove non-letter characters
        content = content.replaceAll("[^a-zA-Z0-9\\s]", "");

        //content = lemmatize(content);

        // Remove "==ABCD==" from text
//        content = content.replaceAll("==.*?==", "");

        // TODO: lemmatize content - comment this
        // Lemmatize the content
//        content = lemmatize(content);

//        int minTokenLength = 3;
//        try (TokenStream stream = new CustomLengthFilter(this.analyzer.tokenStream("content", content), minTokenLength)) {
//        try (TokenStream stream = this.analyzer.tokenStream("content", content)) {
//            StringBuilder result = new StringBuilder();
//            CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);
//
//            stream.reset();
//            while (stream.incrementToken()) {
//                result.append(charTermAttribute.toString()).append(" ");
//            }
//            stream.end();
//
//            return result.toString().trim();
//        }
        return content;
    }

    static class CustomLengthFilter extends org.apache.lucene.analysis.FilteringTokenFilter {
        private final int minTokenLength;
        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        public CustomLengthFilter(TokenStream input, int minTokenLength) {
            super(input);
            this.minTokenLength = minTokenLength;
        }

        @Override
        protected boolean accept() {
            return (termAtt.length() > minTokenLength);
        }
    }
}
