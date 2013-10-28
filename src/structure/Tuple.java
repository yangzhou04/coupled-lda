package structure;

import java.util.ArrayList;
import java.util.List;

public class Tuple {
    /**
     * subject
     */
    public String subj;
    /**
     * object
     */
    public String obj;
    /**
     * predicate
     */
    public String pred;
    /**
     * context
     */
    public List<String> context;

    public Tuple() {
        super();
        context = new ArrayList<String>();
    }

    public Tuple(String subj, String obj, String pred, List<String> context) {
        this();
        this.subj = subj;
        this.obj = obj;
        this.pred = pred;
        this.context = context;
    }

    public Tuple(Tuple that) {
        this(that.subj, that.obj, that.pred, null);
    }

    public boolean isValid() {
        return pred != null && (subj != null || obj != null);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(subj);
        sb.append("\t");
        sb.append(pred);
        sb.append("\t");
        sb.append(obj);
        return sb.toString();
    }

}
