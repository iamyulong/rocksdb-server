package com.ranksays.rocksdb.server;

import java.io.File;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.rocksdb.RocksDB;

/**
 * Launcher for RocksDB Server
 *
 */
public class Main {
	private static final Logger logger = LogManager.getLogger(Main.class);

	private static Map<String, RocksDB> databases = new HashMap<>();

	public static void main(String[] args) throws Exception {

		if (Configs.load()) {
			// Load RocksDB static libarary
			RocksDB.loadLibrary();

			// Create data folder if missing
			new File(Configs.dataDir).mkdirs();

			// Register shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					synchronized (databases) {
						for (RocksDB db : databases.values()) {
							if (db != null) {
								db.close();
							}
						}
						logger.info("Server shut down.");
					}
				}
			});

			// Create Server
			InetSocketAddress addr = new InetSocketAddress(Configs.listen, Configs.port);
			Server server = new Server(addr);
			server.setHandler(new HttpHandler(databases));

			try {
				server.start();
				logger.info("Server started.");
				server.join();
			} catch (BindException e) {
				logger.error("Failed to bind at " + addr + ": " + e.getMessage());
				System.exit(-1);
			}
		}
	}
}
