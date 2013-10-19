package conventer;

import static util.Utils.read;
import static util.Utils.write;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import data.Document;
import flag.SemanticRoleType;

/**
 * Indexing word
 * 
 * @author ZhouYang
 * 
 */
public class IndexConventer {
    /**
     * Mapping word to int
     */
    private Map<String, Integer> word2Int;
    /**
     * Mapping int back to word
     */
    private Map<Integer, String> int2Word;
    /**
     * Mapping
     */
    private Map<Integer, SemanticRoleType> int2Role;
    private int index = 0;
    private int tupleCount;
    private int subjectCount;
    private int verbCount;
    private int objectCount;

    static class ConventerHolder {
        static IndexConventer instance = new IndexConventer();
    }

    private IndexConventer() {
        word2Int = new HashMap<String, Integer>();
        int2Word = new HashMap<Integer, String>();
        int2Role = new HashMap<Integer, SemanticRoleType>();
    }

    public static IndexConventer getInstance() {
        return ConventerHolder.instance;
    }

    public int[][] mapping(List<Document> documents) throws IOException {
        StringBuffer sb = new StringBuffer();
        // String to int mapping
        int[][] mapped = new int[documents.size()][];
        tupleCount = 0;// statistic tuple count

        for (int d = 0; d < documents.size(); d++) {
            Document doc = documents.get(d);
            // Separate file into tuples
            StringTokenizer st = new StringTokenizer(doc.getContent(), "\n");
            tupleCount += st.countTokens();
            List<Integer> tmp = new LinkedList<Integer>();
            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                // Separate a tuple into SVO
                StringTokenizer st2 = new StringTokenizer(line, "\t");
                int tokenidx = 0;
                while (st2.hasMoreTokens()) {
                    String k = st2.nextToken();
                    // System.out.println(tokenidx + ": " + k);
                    // if (k.compareTo("null") == 0) {
                    // tokenidx++;
                    // continue;
                    // }
                    // if find a new word
                    if (!word2Int.containsKey(k)) {
                        word2Int.put(k, index);
                        int2Word.put(index, k);
                        // statistic SVO count
                        if (tokenidx % 3 == 0) {
                            subjectCount++;
                            int2Role.put(index, SemanticRoleType.SUBJ);
                        } else if (tokenidx % 3 == 1) {
                            verbCount++;
                            int2Role.put(index, SemanticRoleType.VERB);
                        } else {
                            objectCount++;
                            int2Role.put(index, SemanticRoleType.OBJ);
                        }
                        index++;
                    }
                    int mappedInteger = word2Int.get(k);
                    tmp.add(mappedInteger);
                    sb.append(mappedInteger);
                    sb.append(",");
                    tokenidx++;
                }
            }
            mapped[d] = new int[tmp.size()];
            for (int k = 0; k < tmp.size(); k++) {
                mapped[d][k] = tmp.get(k);
            }

            write("data/exper.mappedtuples/" + doc.getLabel(), sb.toString());
            sb.delete(0, sb.length());
        }
        return mapped;
    }

    public boolean isNull(String word) {
        return word.compareTo("null") == 0;
    }

    public boolean isNull(int idx) {
        return idx == getNullIndex();
    }

    public boolean isSubject(int idx) {
        return int2Role.get(idx) == SemanticRoleType.SUBJ;
    }

    public boolean isObject(int idx) {
        return int2Role.get(idx) == SemanticRoleType.OBJ;
    }

    public boolean isVerb(int idx) {
        return int2Role.get(idx) == SemanticRoleType.VERB;
    }

    public SemanticRoleType getRoleType(int idx) {
        return int2Role.get(idx);
    }

    // public String getRole(int idx) {
    // return int2Role.get(idx).toString();
    // }

    public int getNullIndex() {
        return word2Int.get("null");
    }

    public int getVocabulary() {
        return int2Word.size();
    }

    public int getSubjectCount() {
        return subjectCount;
    }

    public int getVerbCount() {
        return verbCount;
    }

    public int getObjectCount() {
        return objectCount;
    }

    public int getTupleNumber() {
        return tupleCount;
    }

    public int word2Index(String word) {
        return word2Int.get(word);
    }

    public String index2Word(int index) {
        return int2Word.get(index);
    }

    public int getWordCount() {
        return int2Word.keySet().size();
    }

    public void save(String path) throws IOException {
        File f = new File(path);
        if (!f.exists())
            f.createNewFile();

        StringBuffer sb = new StringBuffer();
        sb.append("Index size is: " + getWordCount());
        sb.append("\n");
        sb.append("Number of tuple is: " + getTupleNumber());
        sb.append("\n");
        sb.append("Word2Int: \n");
        sb.append(word2Int.toString());
        sb.append("\n\nInt2Word: \n");
        sb.append(int2Word.toString());

        write(f, sb.toString());
    }

    public static void main(String[] args) throws IOException {
        IndexConventer c = IndexConventer.getInstance();

        String folder = "data/exper.tuples/";

        List<Document> documents = new ArrayList<Document>();
        File f = new File(folder);
        for (String fn : f.list()) {
            documents.add(new Document(fn, read(folder + fn)));
        }
        c.mapping(documents);
        c.save("./data/mapping");
    }

}
