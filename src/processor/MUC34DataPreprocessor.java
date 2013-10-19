package processor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import util.Utils;

public class MUC34DataPreprocessor {

    public static void main(String[] args) throws IOException {
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
}
