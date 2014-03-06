package processor;

import static util.Utils.write;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import structure.Document;
import flag.SemanticRoleType;

/**
 * Indexing word
 * 
 * @author Yang Zhou
 * 
 */
public class Indexr {
    /**
     * Mapping word to int
     */
    private Map<String, Integer> word2Int;
    private Map<String, Integer> subject2Int;
    private Map<String, Integer> predicate2Int;
    private Map<String, Integer> object2Int;
    /**
     * Mapping int back to word
     */
    private Map<Integer, String> int2Word;
    /**
     * Mapping
     */
    private Map<Integer, SemanticRoleType> int2Role;
//    private int index = 0;
    private int tupleCount;
//    private int subjectCount;
//    private int predicateCount;
//    private int objectCount;

    static class ConventerHolder {
        static Indexr instance = new Indexr();
    }

    private Indexr() {
        word2Int = new HashMap<String, Integer>();
        subject2Int =new HashMap<String, Integer>(); 
        predicate2Int =new HashMap<String, Integer>();
        object2Int =new HashMap<String, Integer>();
        int2Word = new HashMap<Integer, String>();
        int2Role = new HashMap<Integer, SemanticRoleType>();
    }

    public static Indexr getInstance() {
        return ConventerHolder.instance;
    }

    public int[][] doIndex(List<Document> corpus) throws IOException {
        // map word to int
        int[][] indexedCorpus = new int[corpus.size()][];
        tupleCount = 0;

        // deal with tuple without specific semantic role.
        word2Int.put("null", 0);
        subject2Int.put("null", 0);
        predicate2Int.put("null", 0);
        object2Int.put("null", 0);
        int index=0,sindex=0, pindex=0, oindex=0;
        for (int d = 0; d < corpus.size(); d++) {
            Document doc = corpus.get(d);
            // Separate file into tuples
            StringTokenizer st = new StringTokenizer(doc.getContent(), "\n");
            tupleCount += st.countTokens();
            List<Integer> indexBuf = new LinkedList<Integer>();
            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                // Separate a tuple into SVO
                StringTokenizer st2 = new StringTokenizer(line, "\t");
                if (st2.countTokens() != 3) {
                    System.err.println("Input data format error. Please check!");
                    System.exit(-1);
                }
                int pos = 0;
                while (st2.hasMoreTokens()) {
                    String k = st2.nextToken().trim();
                    // System.out.println(tokenidx + ": " + k);
                    // if (k.compareTo("null") == 0) {
                    // tokenidx++;
                    // continue;
                    // }
                    // if find a new word
                    if (!word2Int.containsKey(k)) {
                        word2Int.put(k, index); int2Word.put(index, k);
                        index++;
                    }
                    if (pos == 0) {
                        if (!subject2Int.containsKey(k)) {
                            subject2Int.put(k, sindex++);
                        }
                    } else if (pos == 1) {
                        if (!predicate2Int.containsKey(k)) {
                            predicate2Int.put(k, pindex++);
                        }
                    } else if (pos == 2) {
                        if (!object2Int.containsKey(k)) {
                            object2Int.put(k, oindex++);
                        }
                    } else
                        assert false;
                    
                    int mappedInteger = word2Int.get(k);
                    indexBuf.add(mappedInteger);
//                    sb.append(mappedInteger);
//                    sb.append(",");
//                    tokenidx++;
                    pos++;
                }
            }
            indexedCorpus[d] = new int[indexBuf.size()];
            for (int k = 0; k < indexBuf.size(); k++) {
                indexedCorpus[d][k] = indexBuf.get(k);
            }
            // write("data/exper.mappedtuples/" + doc.getLabel(), sb.toString());
//            sb.delete(0, sb.length());
        }
        return indexedCorpus;
    }

    public boolean isNull(String word) {
        return word.compareTo("null") == 0;
    }

    public boolean isNull(int idx) {
        return idx == getNullIndex();
    }

    public boolean isSubject(int idx) {
        return int2Role.get(idx) == SemanticRoleType.SUBJECT;
    }

    public boolean isObject(int idx) {
        return int2Role.get(idx) == SemanticRoleType.OBJECT;
    }

    public boolean isVerb(int idx) {
        return int2Role.get(idx) == SemanticRoleType.PREDICATE;
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

    public int getVocabularySize() {
        return int2Word.size();
    }

    public int getSubjectSize() {
        return subject2Int.size();
    }

    public int getPredicateCount() {
        return predicate2Int.size();
    }

    public int getObjectCount() {
        return object2Int.size();
    }

    public int getTupleCount() {
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
        sb.append("Number of tuple is: " + getTupleCount());
        sb.append("\n");
        sb.append("Word2Int: \n");
        sb.append(word2Int.toString());
        sb.append("\n\nInt2Word: \n");
        sb.append(int2Word.toString());

        write(f, sb.toString());
    }

    public static void main(String[] args) throws IOException {
        // IndexProcessor c = IndexProcessor.getInstance();
        //
        // String folder = "data/exper.tuples/";
        //
        // List<Document> documents = new ArrayList<Document>();
        // File f = new File(folder);
        // for (String fn : f.list()) {
        // documents.add(new Document(fn, read(folder + fn)));
        // }
        // c.mapping(documents);
        // c.save("./data/mapping");
    }

}
