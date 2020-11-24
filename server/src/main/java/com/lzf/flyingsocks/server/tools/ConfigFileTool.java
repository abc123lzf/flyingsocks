package com.lzf.flyingsocks.server.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lzf.flyingsocks.util.BaseUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

/**
 * 用于构建初始配置文件
 */
public final class ConfigFileTool {

    private static final File CFG_PATH;

    private static final String CFG_FILE_NAME = "config.json";

    static {
        try {
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }

        Properties p = new Properties();
        try {
            URL url = new URL("classpath://config.properties");
            p.load(url.openStream());
        } catch (IOException e) {
            throw new Error(e);
        }

        if (Platform.isWindows()) {
            CFG_PATH = new File(p.getProperty("config.location.windows"));
        } else if (Platform.isMacOSX()) {
            CFG_PATH = new File(p.getProperty("config.location.mac"));
        } else if (Platform.isLinux()) {
            CFG_PATH = new File(p.getProperty("config.location.linux"));
        } else {
            throw new Error("Unknown Operation system");
        }

        if (!CFG_PATH.exists() && CFG_PATH.mkdirs()) {
            throw new Error("Can not make directory at " + CFG_PATH.getAbsolutePath());
        } else if (CFG_PATH.isFile()) {
            throw new Error(CFG_PATH.getAbsolutePath() + " was a file, It's should be directory.");
        }
    }


    public static void main(String[] args) {
        if (args.length == 0) {
            help();
            return;
        }

        switch (args[0]) {
            case "-create":
                create();
                break;
            case "-list":
                list();
                break;
            case "-remove":
                remove();
                break;
            default:
                help();
                return;
        }

        println("Done.");
    }

    private static void create() {
        JSONArray src = loadConfigFile();
        if (src == null) {
            src = new JSONArray();
        }

        Scanner sc = new Scanner(System.in);
        print("Enter this server config's name: ");
        String name = sc.next();
        if (name.isEmpty()) {
            System.err.println("Name should not null");
            return;
        }

        for (int i = 0; i < src.size(); i++) {
            if (src.getJSONObject(i).getString("name").equals(name)) {
                System.err.println("Name \"" + name + "\" is already exists on config.json");
                return;
            }
        }

        print("Enter port [1 ~ 65535]: ");
        int port = sc.nextInt();
        if (!BaseUtils.isPort(port)) {
            System.err.println("Illegal port.");
            return;
        }

        print("Enter max clients [more than 1]: ");
        int maxClient = sc.nextInt();
        if (maxClient < 1) {
            System.err.println("Illegal max client number.");
            return;
        }

        print("Enter encrypt [0 or 1] 0 is not using encrypt, 1 is SSL: ");
        int encrypt = sc.nextInt();

        if (encrypt != 0 && encrypt != 1) {
            System.err.println("Illegal encrypt type.");
            return;
        }

        int cport = 0;
        if (encrypt == 1) {
            print("SSL Cert port [1 ~ 65535]: ");
            cport = sc.nextInt();
            if (cport == port) {
                System.err.println("This port should not same as proxy port.");
            } else if (!BaseUtils.isPort(cport)) {
                System.err.println("Illegal port.");
            }
        }

        print("Enter auth type [0 or 1] 0 is simple authentication, 1 is user authentication: ");
        int auth = sc.nextInt();
        if (auth != 0 && auth != 1) {
            System.err.println("Illegal auth type.");
            return;
        }


        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("port", port);
        if (encrypt == 1) {
            obj.put("cert-port", cport);
        }
        obj.put("max-client", maxClient);
        obj.put("encrypt", encrypt == 1 ? "OpenSSL" : "None");
        obj.put("auth-type", auth == 1 ? "simple" : "user");

        if (auth == 0) {
            print("Enter password [more than 6 character]:");
            String pass = sc.next();
            if (pass.length() < 6) {
                System.err.println("Password length should be more than 6 character");
                return;
            }

            obj.put("password", pass);

        } else {
            print("Enter user group:");
            String group = sc.next();
            obj.put("group", group);
        }

        src.add(obj);
        writeConfigFile(src);
    }


    private static void remove() {
        JSONArray arr = loadConfigFile();
        if (arr == null || arr.isEmpty()) {
            System.err.println("No config found.");
            return;
        }

        print("Enter the config name:");
        Scanner sc = new Scanner(System.in);
        String name = sc.next();

        for (int i = 0; i < arr.size(); i++) {
            if (arr.getJSONObject(i).getString("name").equals(name)) {
                arr.remove(i);
                writeConfigFile(arr);
                println("Remove success.");
                return;
            }
        }

        println("No config name \"" + name + "\" found.");
    }

    private static void list() {
        JSONArray arr = loadConfigFile();
        if (arr == null || arr.isEmpty()) {
            println("No config found.");
            return;
        }

        println(arr.toString(SerializerFeature.PrettyFormat));
    }


    private static JSONArray loadConfigFile() {
        File f = new File(CFG_PATH, CFG_FILE_NAME);
        if (!f.exists()) {
            return null;
        }

        try {
            char[] c = new char[(int) f.length()];
            FileReader fr = new FileReader(f);
            int cnt = fr.read(c);
            fr.close();
            return JSON.parseArray(new String(c, 0, cnt));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static void writeConfigFile(JSONArray arr) {
        File f = new File(CFG_PATH, CFG_FILE_NAME);
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(arr.toString(SerializerFeature.PrettyFormat));
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private static void help() {
        println("You can use this tools to manage flyingsocks server config");
        println("Usage:");
        println("First argument must be -create / -remove / -list");
    }

    private static void print(String str) {
        System.out.print(str);
    }

    private static void println(String str) {
        System.out.println(str);
    }


    private ConfigFileTool() {
    }
}
