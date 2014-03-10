package processor;

import static util.Utils.write;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
public class MultiRoleIndexr {
    private static String nullString = "null";
    /**
     * Mapping word to int
     */
    private Map<String, Integer> word2Int;
    private Map<String, Integer>[] role2Int;
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

    public MultiRoleIndexr() {
        word2Int = new HashMap<String, Integer>();
        int2Word = new HashMap<Integer, String>();
        int2Role = new HashMap<Integer, SemanticRoleType>();
        
    }

    @SuppressWarnings("unchecked")
    public int[][] doIndex(List<Document> corpus, int roleNum) throws IOException {
        // map word to int
        int[][] indexedCorpus = new int[corpus.size()][];
        tupleCount = 0;

        // deal with tuple without specific semantic role.
        word2Int.put(nullString, 0);
        
        role2Int = new Map[roleNum];
        
        for (int i = 0; i < roleNum; i++)
            role2Int[i] = new HashMap<String, Integer>();
        int[] roleIndex = new int[roleNum];
        
        int index=0;
        for (int d = 0; d < corpus.size(); d++) {
            Document doc = corpus.get(d);
            // Separate file into tuples
            StringTokenizer st = new StringTokenizer(doc.getContent(), "\n");
            tupleCount += st.countTokens();
            int[] indexBuf = new int[roleNum*tupleCount];
            for (int lineno = 0;st.hasMoreTokens();lineno++) {
                String line = st.nextToken();
                // Separate a tuple into SVO
                StringTokenizer st2 = new StringTokenizer(line, "\t");
                if (st2.countTokens() != 3) {
                    System.err.println("Input data format error. Please check!");
                    System.exit(-1);
                }
                for (int pos=0; st2.hasMoreTokens(); pos++) {
                    String k = st2.nextToken().trim();

                    // if find a new word
                    if (!word2Int.containsKey(k)) {
                        word2Int.put(k, index); int2Word.put(index, k);
                        index++;
                    }

                    if (!role2Int[pos].containsKey(k))
                        role2Int[pos].put(k, ++roleIndex[pos]);

                    indexBuf[lineno*roleNum + pos] = word2Int.get(k);
                }
            }
            indexedCorpus[d] = indexBuf;
        }
        return indexedCorpus;
    }

    public boolean isNull(String word) {
        return word.compareTo(nullString) == 0;
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
        return word2Int.get(nullString);
    }

    public int getVocabularySize() {
        return int2Word.size();
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
    
    public int getRoleCount(int i) {
        return role2Int[i].size();
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

}
