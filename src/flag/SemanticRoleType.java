package flag;

public enum SemanticRoleType {
    SUBJ("subject"), VERB("verb"), OBJ("object");
    private String role;

    SemanticRoleType(String role) {
        this.role = role;
    }

    public String toString() {
        return role;
    }
}
