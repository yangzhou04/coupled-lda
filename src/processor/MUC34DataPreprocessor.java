package processor;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import util.Utils;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.util.CoreMap;

public class MUC34DataPreprocessor {
    private static FilenameFilter filenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String subfile) {
            return subfile.contains("dev-muc3-");
        }
    };

    public static void betaInfoFilter() throws IOException {
        String dir = "./data/muc34-dev";
        File dirFile = new File(dir);
        Pattern p = Pattern.compile("([A-Za-z0-9 ,()-]* ?--)|"
                + "(DEV-MUC3-\\d{4} \\([A-Za-z ]*\\))|"
                + "(\\[[A-Za-z0-9 \\n,]*?\\])");

        File[] files = dirFile.listFiles(filenameFilter);

        for (File file : files) {
            String content = Utils.read(file);
            content = p.matcher(content).replaceAll("");
            Utils.write(new File("./data/muc34-beta/", file.getName()), content);
        }
    }

    public static void neFilter() throws IOException {
        String dir = "./data/muc34-beta";
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles(filenameFilter);

        String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";

        AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier
                .getClassifierNoExceptions(serializedClassifier);

        for (File file : files) {
            String fileContents = IOUtils.slurpFile(file);
            StringBuilder sb = new StringBuilder();
            List<List<CoreLabel>> doc = classifier.classify(fileContents);
            for (List<CoreLabel> sentence : doc) {
                for (int i = 0; i < sentence.size(); i++) {
                    CoreLabel word = sentence.get(i);
                    String type = word
                            .get(CoreAnnotations.AnswerAnnotation.class);
                    String content = word.word();
                    switch (type) {
                    case "LOCATION":
                        sb.append("LOCATION");
                        break;
                    case "PERSON":
                        sb.append("PERSON");
                        break;
                    case "ORGANIZATION":
                        sb.append("ORGANIZATION");
                        break;
                    case "O":
                        sb.append(content);
                        break;
                    default:
                        System.out.println(type + ": " + content);
                        System.exit(-1);
                        break;
                    }
                    sb.append(" ");

                    while (!type.equals("O")
                            && i + 1 < sentence.size()
                            && sentence
                                    .get(i + 1)
                                    .get(CoreAnnotations.AnswerAnnotation.class)
                                    .equals(type)) {
                        i++;
                    }
                } // end sentence

                sb.deleteCharAt(sb.length() - 1);
                sb.append("\n");
            } // end document
            Utils.write(new File("./data/muc34-ne/", file.getName()),
                    sb.toString());
        } // end corpus
    }

    public static void lemmaFilter() throws IOException {
        String dir = "./data/muc34-ne";
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles(filenameFilter);

        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props, false);

        for (File file : files) {
            String fileContents = IOUtils.slurpFile(file);
            Annotation document = pipeline.process(fileContents);
            StringBuilder sb = new StringBuilder();
            for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
                for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                    String lemma = token.get(LemmaAnnotation.class);
                    sb.append(lemma);
                    sb.append(" ");
                } // end sentence
                sb.deleteCharAt(sb.length() - 1);
                sb.append("\n");
            } // end document
            Utils.write(new File("./data/muc34-lemma/", file.getName()),
                    sb.toString());
        } // end corpus
    }

    public static void svoFilter() throws IOException {
        String dir = "./data/muc34-lemma";
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles(filenameFilter);

        String grammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        String[] options = { "-maxLength", "80", "-retainTmpSubcategories" };
        LexicalizedParser lp = LexicalizedParser.loadModel(grammar, options);
        TreebankLanguagePack tlp = lp.getOp().langpack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

        for (File file : files) {
            // String fileContents = IOUtils.slurpFile(file);
            StringBuilder sb = new StringBuilder();

            DocumentPreprocessor dp = new DocumentPreprocessor(new FileReader(
                    file));
            Iterable<List<? extends HasWord>> sentences;
            List<List<? extends HasWord>> tmp = new ArrayList<List<? extends HasWord>>();
            for (List<HasWord> sentence : dp) {
                tmp.add(sentence);
            }
            sentences = tmp;

            for (List<? extends HasWord> sentence : sentences) {
                
            }
            Utils.write(new File("./data/muc34-tuple/", file.getName()),
                    sb.toString());
        }
    }

    public static void main(String[] args) throws IOException {
        // neFilter();

        lemmaFilter();

        // do sentence dependency parsing to get subject and object of the verb
        // String grammar = args.length > 0 ? args[0]
        // : "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        // String[] options = { "-maxLength", "80", "-retainTmpSubcategories" };
        // LexicalizedParser lp = LexicalizedParser.loadModel(grammar, options);
        // TreebankLanguagePack tlp = lp.getOp().langpack();
        // GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        //
        // Iterable<List<? extends HasWord>> sentences;
        // DocumentPreprocessor dp = new DocumentPreprocessor(
        // "./data/muc34-beta/dev-muc3-0001-0100");
        // List<List<? extends HasWord>> tmp = new ArrayList<List<? extends
        // HasWord>>();
        // for (List<HasWord> sentence : dp) {
        // tmp.add(sentence);
        // }
        // sentences = tmp;
        //
        // for (List<? extends HasWord> sentence : sentences) {
        // Tree parse = lp.parse(sentence);
        //
        // parse.pennPrint(out);
        // GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        // Collection<TypedDependency> roots = GrammaticalStructure
        // .getRoots(gs.typedDependenciesCCprocessed());
        // out.append(roots.toString());
        //
        // // List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
        // //
        // // System.out.println(sentences);
        // // for (TypedDependency td : tdl) {
        // // System.out.println(td);
        // // }
        // }

    }
}
