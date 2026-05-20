package quizgame;

public class Question {
    private String question;
    private String[] options;
    private int correctIndex;

    public Question(String question, String[] options, int correctIndex) {
        this.question = question;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getQuestion() { return question; }
    public String[] getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }

    public boolean isCorrect(int selectedIndex) {
        return selectedIndex == correctIndex;
    }
}
