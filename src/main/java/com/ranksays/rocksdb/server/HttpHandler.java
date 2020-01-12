package com.ranksays.rocksdb.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/**
 * HTTP requests handler
 * 
 */
public class HttpHandler extends AbstractHandler {

    private static final Logger logger = LogManager.getLogger(HttpHandler.class);

    /**
     * Default encoding
     */
    protected static final String ENCODING = "UTF-8";

    /**
     * Opened databases
     */
    protected Map<String, RocksDB> openDBs;

    /***
     * Construct a HttpHandler.
     * 
     * @param databases
     *            opened databases
     */
    public HttpHandler(Map<String, RocksDB> databases) {
        super();
        this.openDBs = databases;
    }

    /**
     * HTTP requests processor, authenticating, parsing and dispatching.
     * 
     * @param target
     *            target URI
     * @param baseRequest
     *            base request
     * @param request
     *            HTTP servlet request
     * @param response
     *            HTTP servlet response
     * 
     * @throws IOException
     * @throws ServletException
     */
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Response resp = null;

        try {
            // parse request
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            BufferedInputStream in = new BufferedInputStream(request.getInputStream());
            for (int c; (c = in.read()) != -1;) {
                buf.write(c);
            }
            JSONObject req = new JSONObject(buf.size() > 0 ? buf.toString(ENCODING) : "{}");

            // authorization (Basic Auth prioritizes)
            String basicAuth = request.getHeader("Authorization");
            if (basicAuth != null && basicAuth.startsWith("Basic ")) {
                req.put("auth", basicAuth.substring(6));
            }

            if ("/get".equals(target)) {
                resp = doGet(req);
            } else if ("/put".equals(target)) {
                resp = doPut(req);
            } else if ("/remove".equals(target)) {
                resp = doRemove(req);
            } else if ("/drop_database".equals(target)) {
                resp = doDropDatabase(req);
            } else if ("/".equals(target)) {
                resp = new Response("This rocksdb-server is working");
            } else {
                resp = new Response(404, "Sorry, that page does not exist");
            }
        } catch (Exception e) {
            resp = new Response(500, "Internal server error");
            logger.error("Internal error: " + e.getMessage());
        }

        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(resp.toJSON());

        baseRequest.setHandled(true);
    }

    /**
     * Get operation handler.
     * 
     * @param req
     *            request JSONObject
     * @return API response
     * @throws IOException
     * @throws ServletException
     */
    protected Response doGet(JSONObject req) throws IOException, ServletException {
        Response resp = new Response();

        // parse parameters & open database
        String db = null;
        byte[][] keys = null;
        RocksDB rdb = null;
        if (!authorize(req, resp) || (db = parseDB(req, resp)) == null || (keys = parseKeys(req, resp)) == null
                || (rdb = openDatabase(db, resp)) == null) {
            return resp;
        }

        try {
            byte[][] values = new byte[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                values[i] = rdb.get(keys[i]);
            }

            JSONArray arr = new JSONArray();
            for (int i = 0; i < keys.length; i++) {
                if (values[i] == null) {
                    arr.put(JSONObject.NULL);
                } else {
                    arr.put(Base64.getEncoder().encodeToString(values[i]));
                }
            }
            resp.setResults(arr);
        } catch (RocksDBException e) {
            resp.setCode(Response.CODE_INTERNAL_ERROR);
            resp.setMessage("Internal server error");

            logger.error("get operation failed: " + e.getMessage());
        }

        return resp;
    }

    /**
     * Put operation handler.
     * 
     * @param req
     *            request JSONObject
     * @return API response
     * @throws IOException
     * @throws ServletException
     */
    protected Response doPut(JSONObject req) throws IOException, ServletException {
        Response resp = new Response();

        // parse parameters & open database
        String db = null;
        byte[][] keys = null, values = null;
        RocksDB rdb = null;
        if (!authorize(req, resp) || (db = parseDB(req, resp)) == null || (keys = parseKeys(req, resp)) == null
                || (values = parseValues(req, resp)) == null || (rdb = openDatabase(db, resp)) == null) {
            return resp;
        }

        if (keys.length != values.length) {
            resp.setCode(Response.CODE_KEY_VALUE_MISMATCH);
            resp.setMessage("Number of key and value does not match");
            return resp;
        }

        try {
            for (int i = 0; i < keys.length; i++) {
                rdb.put(keys[i], values[i]);
            }
        } catch (RocksDBException e) {
            resp.setCode(Response.CODE_INTERNAL_ERROR);
            resp.setMessage("Internal server error");

            logger.error("put operation failed: " + e.getMessage());
        }

        return resp;
    }

    /**
     * remove operation handler.
     * 
     * @param req
     *            request JSONObject
     * @return API response
     * @throws IOException
     * @throws ServletException
     */
    protected Response doRemove(JSONObject req) throws IOException, ServletException {
        Response resp = new Response();

        // parse parameters & open database
        String db = null;
        byte[][] keys = null;
        RocksDB rdb = null;
        if (!authorize(req, resp) || (db = parseDB(req, resp)) == null || (keys = parseKeys(req, resp)) == null
                || (rdb = openDatabase(db, resp)) == null) {
            return resp;
        }

        try {
            for (int i = 0; i < keys.length; i++) {
                rdb.remove(keys[i]);
            }
        } catch (RocksDBException e) {
            resp.setCode(Response.CODE_INTERNAL_ERROR);
            resp.setMessage("Internal server error");

            logger.error("remove operation failed: " + e.getMessage());
        }

        return resp;
    }

    /**
     * Drop database operation handler.
     * 
     * @param req
     *            request JSONObject
     * @return API response
     * @throws IOException
     * @throws ServletException
     */
    protected Response doDropDatabase(JSONObject req) throws IOException, ServletException {
        Response resp = new Response();

        // parse parameters
        String db = null;
        if (!authorize(req, resp) || (db = parseDB(req, resp)) == null) {
            return resp;
        }

        synchronized (openDBs) {
            if (openDBs.containsKey(db)) {
                openDBs.get(db).close();
                openDBs.remove(db);
            }

            try {
                File f = new File(Configs.dataDir, db);
                deleteFile(f);
            } catch (IOException e) {
                resp.setCode(Response.CODE_INTERNAL_ERROR);
                resp.setMessage("Internal server error");

                logger.error("dropping database failed: " + e.getMessage());
            }
        }

        return resp;
    }

    /**
     * Basic authorization
     * 
     * @param req
     *            HTTP request header
     * @return true if authorized otherwise false.
     */
    private boolean authorize(JSONObject req, Response resp) {
        if (Configs.authEnabled) {
            if (!req.isNull("auth")) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(req.getString("auth"));
                    String[] tokens = new String(decoded, ENCODING).split(":");

                    if (Configs.username.equals(tokens[0]) && Configs.password.equals(tokens[1])) {
                        return true;
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
        } else {
            return true;
        }

        resp.setCode(Response.CODE_UNAUTHORIZED);
        resp.setMessage("Not authorized");
        return false;
    }

    /**
     * Parse database name from the request.
     * 
     * @param req
     *            request body
     * @param resp
     *            response container
     * @return a valid DB name; or null if the 'db' key does not exist or the
     *         corresponding value is JSONObject.NULL or does not match the
     *         specifications.
     */
    private String parseDB(JSONObject req, Response resp) {

        if (!req.isNull("db")) {
            String db = req.getString("db");

            if (db.matches("[-_.A-Za-z0-9]+")) {
                return db;
            }
        }

        resp.setCode(Response.CODE_INVALID_DB_NAME);
        resp.setMessage("Invalid database name");
        return null;
    }

    /**
     * parse key(s) from the request.
     * 
     * @param req
     *            request body
     * @param resp
     *            response container
     * @return a byte array; or null if the 'keys' key does not exist or the
     *         corresponding value is JSONObject.NULL or is not a JSONArray of
     *         Base64-encoded strings.
     */
    private byte[][] parseKeys(JSONObject req, Response resp) {

        if (!req.isNull("keys")) {
            try {
                JSONArray arr = req.getJSONArray("keys");

                byte[][] result = new byte[arr.length()][];
                for (int i = 0; i < arr.length(); i++) {
                    result[i] = Base64.getDecoder().decode(arr.getString(i));
                }
                return result;
            } catch (Exception e) {
                // do nothing
            }
        }

        resp.setCode(Response.CODE_INVALID_KEY);
        resp.setMessage("Invalid key(s)");
        return null;
    }

    /**
     * parse value(s) from the request.
     * 
     * @param req
     *            request body
     * @param resp
     *            response container
     * @return a byte array; or null if the 'values' key does not exist or the
     *         corresponding value is JSONObject.NULL or is not a JSONArray of
     *         Base64-encoded strings.
     */
    private byte[][] parseValues(JSONObject req, Response resp) {

        if (!req.isNull("values")) {
            try {
                JSONArray arr = req.getJSONArray("values");

                byte[][] result = new byte[arr.length()][];
                for (int i = 0; i < arr.length(); i++) {
                    result[i] = Base64.getDecoder().decode(arr.getString(i));
                }
                return result;
            } catch (Exception e) {
                // do nothing
            }
        }

        resp.setCode(Response.CODE_INVALID_VALUE);
        resp.setMessage("Invalid value(s)");
        return null;
    }

    /**
     * Open a specified database (create if missing).
     * 
     * @param db
     *            the DB name
     * @param resp
     *            response container
     * @return the RocksDB reference or null if failed.
     */
    private RocksDB openDatabase(String db, Response resp) {
        synchronized (openDBs) {
            if (openDBs.containsKey(db)) {
                return openDBs.get(db);
            } else {
                Options opts = new Options();
                opts.setCreateIfMissing(true);

                RocksDB rdb = null;
                try {
                    rdb = RocksDB.open(opts, Configs.dataDir + "/" + db);
                    openDBs.put(db, rdb);
                    return rdb;
                } catch (RocksDBException e) {
                    String msg = "Failed to open database: " + e.getMessage();
                    logger.error(msg);

                    resp.setCode(Response.CODE_FAILED_TO_OPEN_DB);
                    resp.setMessage(msg);
                }
                // If user doesn't call options dispose explicitly, then
                // this options instance will be GC'd automatically.
                // opts.close();
            }
        }

        return null;
    }

    /**
     * Delete file or directory recursively.
     * 
     * @param f
     *            file or directory to delete
     * @throws IOException
     *             failed to delete a file
     */
    private void deleteFile(File f) throws IOException {
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    deleteFile(c);
                }
            }
            if (!f.delete()) {
                throw new IOException("Failed to delete file: " + f);
            }
        }
    }
}
