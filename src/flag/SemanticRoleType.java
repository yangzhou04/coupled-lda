package flag;

public enum SemanticRoleType {
    SUBJECT("subject"), PREDICATE("predicate"), OBJECT("object"), CONTEXT("context");
    private String role;

    SemanticRoleType(String role) {
        this.role = role;
    }

    public String toString() {
        return role;
    }
}
