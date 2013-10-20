package processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import structure.Tuple;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * Translate a document to tuples
 * 
 * @author Yang Zhou
 * 
 */
public class DocumentProcessor {

    private LexicalizedParser lp;
    private TreebankLanguagePack tlp;
    private GrammaticalStructureFactory gsf;

    public DocumentProcessor(String grammar, String[] options) {
        lp = LexicalizedParser.loadModel(grammar, options);
        tlp = lp.getOp().langpack();
        gsf = tlp.grammaticalStructureFactory();
    }

    public List<Tuple> doc2Tuple(String docPath) throws IOException {
        DocumentPreprocessor dp = new DocumentPreprocessor(docPath);
        List<List<? extends HasWord>> sentences = new ArrayList<List<? extends HasWord>>();
        for (List<HasWord> sentence : dp) {
            sentences.add(sentence);
        }

        List<Tuple> tuples = new ArrayList<Tuple>();

        for (List<? extends HasWord> sentence : sentences) {
            Tree parse = lp.parse(sentence);
            // parse.pennPrint();
            System.out.println();
            GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
            List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
            System.out.println(tdl);
            Tuple t = new Tuple();
            for (TypedDependency td : tdl) {
                if (td.reln().getShortName().contains("root")) {
                    String pred = td.dep().toString();
                    t.pred = pred;
                }
            }
            for (TypedDependency td : tdl) {
                if (td.reln().getShortName().contains("nsubj")) {
                    String pred = td.gov().toString();
                    if (pred.equals(t.pred))
                        t.subj = td.dep().toString();
                }
                if (td.reln().getShortName().contains("dobj")) {
                    String pred = td.gov().toString();
                    if (pred.equals(t.pred))
                        t.obj = td.dep().toString();
                }
            }
            if (t.subj != null || t.obj != null) {
                t.pred = t.pred.substring(0, t.pred.indexOf('-'));
                if (t.subj != null)
                    t.subj = t.subj.substring(0, t.subj.indexOf('-'));
                if (t.obj != null)
                    t.obj = t.obj.substring(0, t.obj.indexOf('-'));
                tuples.add(t);
            }
            System.out.println(t);
        }

        return tuples;
    }

    public static void main(String[] args) throws IOException {
        String grammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        String[] options = { "-maxLength", "80", "-retainTmpSubcategories" };
        DocumentProcessor tupleConventer = new DocumentProcessor(grammar,
                options);
        tupleConventer.doc2Tuple("./data/muc34-filtered/dev-muc3-0001-0100");
    }
}
