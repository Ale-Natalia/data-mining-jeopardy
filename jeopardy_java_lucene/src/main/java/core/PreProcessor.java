package core;

import edu.stanford.nlp.simple.Sentence;

import java.util.List;

public class PreProcessor {

    public static String lemmatize(String content) {
        if (content == null || content.equals("") || content.equals("[]")) {
            return content;
        }
        Sentence sentence = null;
        try {
            sentence = new Sentence(content);
        } catch (Exception ignored) {
        }
        if (sentence == null) {
            return content;
        }
        List<String> lemmas = sentence.lemmas();

        return String.join(" ", lemmas);
    }

    public String preprocessText(String content) {
        content = content.toLowerCase();
        content = content.replaceAll("[^a-zA-Z0-9\\s]", "");
        content = lemmatize(content);
        return content;
    }

}
