package com.ranksays.rocksdb.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.rocksdb.RocksDB;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    private static ObjectMapper mapper = new ObjectMapper();
    private static final String configFile = "conf/server.json";

    public static void main(String[] args) {
        // Load config file
        Config config = null;
        try {
            config = mapper.readValue(new File(configFile), Config.class);
        } catch (IOException e) {
            logger.error("Failed to load config file: {}", configFile, e);
            System.exit(1);
        }

        // Load RocksDB static library
        RocksDB.loadLibrary();

        // Create data folder and database manager
        new File(config.getDataDir()).mkdirs();
        DatabaseManager manager = new DatabaseManager(config);

        // Launch a web server
        InetSocketAddress address = new InetSocketAddress(config.getNode().getHost(), config.getNode().getPort());
        Server server = new Server(address);
        server.setHandler(new HttpHandler(config, manager));
        try {
            server.start();
            logger.info("RocksDB Server started, visit http://{}:{}", config.getNode().getHost(), config.getNode().getPort());
            server.join();
        } catch (Exception e) {
            logger.error("Failed to start RocksDB Server", e);
            System.exit(2);
        }

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            manager.close();
            try {
                server.stop();
                server.join();
                logger.info("RocksDB Server stopped.");
            } catch (Exception e) {
                logger.error("Failed to stop RocksDB Server", e);
            }
        }));
    }
}
