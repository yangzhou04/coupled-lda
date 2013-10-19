package structure;

public class Word {
    private String label;
    private String content;

    public Word(String label) {
        super();
        this.label = label;
    }

    public Word(String label, String content) {
        super();
        this.label = label;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Word [label=" + label + ", content=" + content + "]";
    }

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
