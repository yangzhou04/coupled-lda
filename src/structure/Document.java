package structure;

public class Document {
    private String label;
    private String content;

    public Document(String label, String content) {
        this.label = label;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Document [label=" + label + ", content=" + content + "]";
    }

    // Getters and Setters
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
