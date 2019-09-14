package com.lzf.flyingsocks.server.tools;

import com.lzf.flyingsocks.util.BaseUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class LogTool {

    private static final String LINUX_PATH = "/var/log/flyingsocks-server";
    private static final String WINDOWS_PATH = "C:\\ProgramData\\flyingsocks-server\\log";
    private static final String MACOS_PATH = " /usr/local/var/log/flyingsocks";

    public static void main(String[] args) throws IOException {
        if(args.length != 2 && args.length != 4 && args.length != 6) {
            help();
            return;
        }

        Map<String, String> map = processArgument(args);

        String folder = null;   //日志文件夹
        String src = null;      //log4j配置文件路径
        String level = null;

        if(map.containsKey("-path")) {
            src = map.get("-path");
        } else {
            help();
            return;
        }

        if(map.containsKey("-folder")) {
            folder = map.get("-folder");
        }

        if(map.containsKey("-level")) {
            level = map.getOrDefault("level", "INFO").toUpperCase(Locale.US);
            if(!isLogLevel(level)) {
                help();
                return;
            }
        }

        doUpdate(src, folder, level);
    }

    private static Map<String, String> processArgument(String[] args) {
        Map<String, String> map = new HashMap<>();
        int n = args.length % 2 == 0 ? args.length : args.length - 1;
        for (int i = 0; i < n; i += 2) {
            map.put(args[i], args[i + 1]);
        }

        return map;
    }


    private static void doUpdate(String src, String folder, String level) throws IOException {
        String logPath = makeDefaultFolder(folder);
        FileInputStream fis = new FileInputStream(src);

        Properties p = new Properties();
        p.load(fis);
        fis.close();

        p.setProperty("log4j.appender.ROLLING_FILE.File", logPath + File.separatorChar + "rolling");
        p.setProperty("log4j.appender.DAILY_ROLLING_FILE.File", logPath + File.separatorChar + "daily");

        String[] output = BaseUtils.splitPreserveAllTokens(p.getProperty("log4j.rootLogger",
                "ROLLING_FILE, DAILY_ROLLING_FILE"), ',');

        StringBuilder sb = new StringBuilder(50);

        for (String s : output) {
            String op = s.trim();
            if(isLogLevel(op)) {
                sb.append(',').append(level);
            } else {
                sb.append(',').append(op);
            }
        }

        p.setProperty("log4j.rootLogger", sb.toString());

        FileOutputStream fos = new FileOutputStream(src);
        p.store(fos, null);
        fos.close();
    }

    private static String makeDefaultFolder(String path) throws IOException {
        File folder;
        if(path == null) {
            if (Platform.isWindows()) {
                folder = new File(WINDOWS_PATH);
            } else if (Platform.isMacOSX()) {
                folder = new File(MACOS_PATH);
            } else {
                folder = new File(LINUX_PATH);
            }
        } else {
            folder = new File(path);
        }

        if(!folder.mkdirs()) {
            throw new IOException("Can not make dir at " + folder.getAbsolutePath());
        }

        return folder.getAbsolutePath();
    }

    private static boolean isLogLevel(String level) {
        return "FATAL".equals(level) || "ERROR".equals(level) ||
                "WARN".equals(level) || "INFO".equals(level) ||
                "DEBUG".equals(level) || "TRACE".equals(level);
    }


    private static void help() {
        System.out.println("LogTool usage:");
        System.out.println("-path    (necessary argument)The file \"log4j.properties\" path");
        System.out.println("-folder  Log file output path\n");
        System.out.println("-level   Log output level, can be FATAL/ERROR/WARN/INFO/DEBUG/TRACE, default is INFO");
        System.out.println("Default log output path:");
        System.out.println(String.format("Linux: %s\nWindows: %s\nMacOSX: %s\n", LINUX_PATH, WINDOWS_PATH, MACOS_PATH));
    }


    private LogTool() { }
}
