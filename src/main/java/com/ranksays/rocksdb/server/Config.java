package com.ranksays.rocksdb.server;

public class Config {
    private Node node;
    private Auth auth;
    private Db db;
    private String dataDir;

    public Node getNode() {
        return node;
    }

    public Auth getAuth() {
        return auth;
    }

    public Db getDb() {
        return db;
    }

    public String getDataDir() {
        return dataDir;
    }

    public class Node {
        private String host;
        private Integer port;

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }
    }

    public class Auth {
        private boolean enabled;
        private String username;
        private String password;

        public boolean isEnabled() {
            return enabled;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public class Db {
        private Integer maxOpenFiles;
        private Integer blockSize;
        private Integer rowCache;
        private Integer writeBufferSize;

        public Integer getMaxOpenFiles() {
            return maxOpenFiles;
        }

        public Integer getBlockSize() {
            return blockSize;
        }

        public Integer getRowCache() {
            return rowCache;
        }

        public Integer getWriteBufferSize() {
            return writeBufferSize;
        }
    }
}
