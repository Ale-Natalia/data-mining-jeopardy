package core;

public class Question {
    String category;
    String clue;
    String answer;

    public Question(String category, String clue, String answer) {
        this.category = category;
        this.clue = clue;
        this.answer = answer;
    }

    @Override
    public String toString() {
        return "Question{" +
                "category='" + category + '\'' +
                ", clue='" + clue + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
