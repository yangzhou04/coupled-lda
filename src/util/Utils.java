package util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public abstract class Utils {

    public static final String PUNCTUATION = " !\"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~";

    public static String read(String path) throws IOException {
        return Utils.read(new File(path));
    }

    public static String read(File f) throws IOException {
        if (!f.canRead())
            throw new IOException("Can not read file: " + f.getAbsolutePath());

        StringBuffer sb = new StringBuffer();
        FileReader fr = new FileReader(f);
        while (fr.ready()) {
            sb.append((char) fr.read());
        }
        fr.close();
        return sb.toString();
    }

    public static void write(String path, String content) throws IOException {
        Utils.write(new File(path), content);
    }

    public static void write(File f, String content) throws IOException {
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        if (!f.canWrite())
            throw new IOException("Can not wrtie file: " + f.getAbsolutePath());

        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.close();
    }
}
