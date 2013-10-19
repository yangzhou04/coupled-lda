package structure;

public class Paragraph {

    private String label;
    private String content;

    public Paragraph(String label, String content) {
        super();
        this.label = label;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Paragraph [label=" + label + ", content=" + content + "]\n";
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
