package structure;

public class Sentence {
    private String content;

    public Sentence(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Sentence [content=" + content + "]";
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
