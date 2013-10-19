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
            // for (TypedDependency td : tdl) {
            // if (td.reln().getShortName().contains("subj")) {
            // System.out.println(td);
            //
            // }
            // if (td.reln().getShortName().contains("obj")) {
            // System.out.println(td);
            // }
            // if (td.reln().getShortName().contains("root")) {
            // System.out.println(td);
            // }
            // }
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
