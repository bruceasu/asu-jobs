package me.asu.jobs;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;

public class Streams {
    public static final Path CRONTAB_PATH = Paths.get(System.getProperty("user.home"), ".config", "cron", "crontab.txt");
    public static final Path AT_PATH = Paths.get(System.getProperty("user.home"), ".config", "cron", "at.txt");

    public static int getWebServerPort() {
        int port = 8080;
        Path configPath = Paths.get(System.getProperty("user.home"), ".config/cron/cron.properties");
        if (Files.isRegularFile(configPath)) {
            Properties properties = new Properties();
            try {
                properties.load(Files.newBufferedReader(configPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            port = Integer.parseInt(properties.getProperty("port", "8080"));
        }
        return port;
    }


    public static String getEditor() {
        String editor = "C:\\windows\\System32\\notepad.exe";
        Path configPath = Paths.get(System.getProperty("user.home"), ".config/cron/cron.properties");
        if (Files.isRegularFile(configPath)) {
            Properties properties = new Properties();
            try {
                properties.load(Files.newBufferedReader(configPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            editor = properties.getProperty("editor", editor);
        }
        return editor;
    }

    public static List<String> getAtList() throws IOException {
        if (!Files.exists(AT_PATH)) {
            Path parent = AT_PATH.toAbsolutePath().getParent();
            if (!Files.isDirectory(parent)) {
                Files.createDirectories(parent);
            }
        }
        List<String> atLines = Files.readAllLines(AT_PATH);
        return atLines;
    }

    public static List<String> getCronList() throws IOException {
        if (!Files.exists(CRONTAB_PATH)) {
            Path parent = CRONTAB_PATH.toAbsolutePath().getParent();
            if (!Files.isDirectory(parent)) {
                Files.createDirectories(parent);
            }
            Files.createFile(CRONTAB_PATH);
        }
        List<String> cronLines = Files.readAllLines(CRONTAB_PATH);
        return cronLines;
    }

    public static void appendToCron(String cron) throws IOException {
        Path parent = CRONTAB_PATH.toAbsolutePath().getParent();
        if (!Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(CRONTAB_PATH, cron.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }

    public static void appendToAt(String at) throws IOException {
        Path parent = AT_PATH.toAbsolutePath().getParent();
        if (!Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(AT_PATH, at.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }

    /**
     * 从一个文本输入流读取所有内容，并将该流关闭
     *
     * @param reader 文本输入流
     * @return 输入流所有内容
     */
    public static String readAll(Reader reader) throws IOException {
        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader);
        }
        StringBuilder sb = new StringBuilder();

        char[] data = new char[64];
        int    len;
        while ((len = reader.read(data)) != -1) {
            sb.append(data, 0, len);
        }
        reader.close();
        return sb.toString();
    }

    public static String toString(InputStream is) throws IOException {
        return toString(is, StandardCharsets.UTF_8);
    }

    public static String toString(InputStream is, Charset cs) throws IOException {
        return readAll(new InputStreamReader(is, cs));
    }
}
