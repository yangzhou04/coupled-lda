package processor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import util.Utils;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

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
        PrintWriter out = new PrintWriter("./test_out.txt");
        String text = Utils.read("./data/muc34-filtered/dev-muc3-0001-0100");
        // String[] words = content.split(" ");
        Map<String, Integer> wordMap = new TreeMap<String, Integer>();

        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props, false);

        Annotation document = pipeline.process(text);
        for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                if (!token.tag().contains("NP") && !token.tag().contains("VB"))
                    continue;
                //String word = token.get(TextAnnotation.class);
                //out.append(word);
                //out.append(": ");
                //out.append(token.tag());
                //out.append(": ");
                String lemma = token.get(LemmaAnnotation.class);
                //out.append(lemma);
                //out.append("\n");

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
            out.append(key + ": " + wordMap.get(key));
            out.append("\n");
        }
        out.close();
    }
}
