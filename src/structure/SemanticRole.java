package structure;

import flag.SemanticRoleType;

public class SemanticRole implements Comparable<SemanticRole> {
    private SemanticRoleType roleType;
    private String word;
    private double value;

    public SemanticRole(SemanticRoleType roleType, String word, double value) {
        super();
        this.roleType = roleType;
        this.word = word;
        this.value = value;
    }

    public SemanticRoleType getRoleType() {
        return roleType;
    }

    public String getWord() {
        return word;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(SemanticRole role) {
        // if value is the same, sorted by word alpha sequence
        if (Double.compare(this.value, role.getValue()) == 0
                && this.word.compareTo(role.getWord()) > 0) {
            return 1;
        } else if (this.value < role.getValue())
            return 1;
        else
            return -1;
    }

}
