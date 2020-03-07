package com.ranksays.rocksdb.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.LRUCache;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseManager implements Closeable {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private AtomicBoolean closed = new AtomicBoolean(false);
    private Config config;
    private Map<String, RocksDB> databases;

    public DatabaseManager(Config config) {
        this.config = config;
        this.databases = new HashMap<>();
    }

    /**
     * Creates a {@link RocksDB} instance for the given database.
     *
     * @param name the name of the database
     * @return a database instance
     * @implNote created instance may be cached and shared among clients.
     */
    public synchronized RocksDB open(String name) {
        if (!databases.containsKey(name) && !closed.get()) {
            Options opts = new Options();
            opts.setCreateIfMissing(true);
            if (config.getDb().getBlockSize() != null) {
                opts.setArenaBlockSize(config.getDb().getBlockSize());
            }
            if (config.getDb().getRowCache() != null) {
                opts.setRowCache(new LRUCache(config.getDb().getRowCache()));
            }
            if (config.getDb().getMaxOpenFiles() != null) {
                opts.setMaxOpenFiles(config.getDb().getMaxOpenFiles());
            }
            if (config.getDb().getWriteBufferSize() != null) {
                opts.setWriteBufferSize(config.getDb().getWriteBufferSize());
            }

            RocksDB db;
            try {
                db = RocksDB.open(opts, config.getDataDir() + "/" + name);
                databases.put(name, db);
                return db;
            } catch (RocksDBException e) {
                logger.error("Failed to open database: name = {}", name, e);
            }
        }

        return databases.get(name);
    }

    /**
     * Closes the {@link RocksDB} instance for the given database, if already opened.
     * <p>
     * All resources retained by the database will be released afterwards.
     *
     * @param name the database name
     */
    public synchronized void close(String name) {
        RocksDB db = databases.remove(name);
        if (db != null) {
            db.close();
        }
    }

    /**
     * Deletes a database if exists.
     *
     * @param name the database name.
     */
    public synchronized void drop(String name) throws IOException {
        close(name);
        deleteFileRecursively(new File(config.getDataDir(), name));
    }

    /**
     * Closes all open {@link RocksDB} instances.
     *
     * @throws IOException when any IO error occurs
     */
    @Override
    public synchronized void close() {
        List<String> names = new ArrayList<>(databases.keySet());
        for (String name : names) {
            close(name);
        }
        closed.set(true);
    }

    private void deleteFileRecursively(File f) throws IOException {
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    deleteFileRecursively(c);
                }
            }
            if (!f.delete()) {
                throw new IOException("Failed to delete file: " + f);
            }
        }
    }
}
