package processor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import util.Utils;

public class MUC34DataPreprocessor {

    public static void removeBetaInfomation() throws IOException {
        String dir = "./data/muc34-dev";
        File dirFile = new File(dir);
        Pattern p = Pattern.compile("([A-Za-z0-9 ,()-]* ?--)|"
                + "(DEV-MUC3-\\d{4} \\([A-Za-z ]*\\))|"
                + "(\\[[A-Za-z0-9 \\n,]*?\\])");

        File[] devFiles = dirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String subfile) {
                return subfile.contains("dev-muc3-");
            }
        });

        for (File file : devFiles) {
            String content = Utils.read(file);
            content = p.matcher(content).replaceAll("");
            Utils.write(new File("./data/muc34-filtered/", file.getName()),
                    content);
        }
    }

    public static void main(String[] args) throws IOException {
        String text = Utils.read("./data/muc34-filtered/dev-muc3-0001-0100");
        // String[] words = content.split(" ");
        Map<String, Integer> wordMap = new TreeMap<String, Integer>();
        // Pattern puncPat = Pattern.compile("[,.\"[]()]$");

        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props, false);
        // String text = "ran";/* the string you want */

        Annotation document = pipeline.process(text);
        for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                System.out.print(word);
                System.out.print(": ");
                String lemma = token.get(LemmaAnnotation.class);
                System.out.println(lemma);
                if (lemma.equals(""))
                    continue;
                if (!wordMap.containsKey(lemma))
                    wordMap.put(lemma, 1);
                else {
                    int count = wordMap.get(lemma);
                    wordMap.put(lemma, ++count);
                }
            }
        }

        for (String key : wordMap.keySet()) {
            System.out.println(key + ": " + wordMap.get(key));
        }
    }
}
