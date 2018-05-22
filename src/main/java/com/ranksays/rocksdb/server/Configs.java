package com.ranksays.rocksdb.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Global configurations
 * 
 */
public class Configs {
    private static final Logger logger = LogManager.getLogger(Configs.class);

    private static final String SERVER_CONFIG_FILE = "conf/server.json";

    public static String listen = "127.0.0.1";
    public static int port = 8080;

    public static boolean authEnabled = false;
    public static String username = null;
    public static String password = null;

    public static String dataDir = "data";

    public static String readFile(String path, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));

        return new String(encoded, encoding);
    }

    public static boolean load() {
        try {
            String configs = readFile(SERVER_CONFIG_FILE, "UTF-8");
            JSONObject obj = new JSONObject(configs);

            if (obj.has("listen")) {
                listen = obj.getString("listen");
            }
            if (obj.has("port")) {
                port = obj.getInt("port");
            }

            if (obj.has("auth")) {
                JSONObject o = obj.getJSONObject("auth");
                authEnabled = o.getBoolean("enabled");
                if (authEnabled) {
                    username = o.getString("username");
                    password = o.getString("password");
                }
            }

            if (obj.has("data_dir")) {
                dataDir = obj.getString("data_dir");
            }
        } catch (IOException | JSONException e) {
            logger.error("Failed to parse " + SERVER_CONFIG_FILE + ": " + e.getMessage());
            return false;
        }

        return true;
    }
}
