package com.ranksays.rocksdb.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HttpHandler extends AbstractHandler {

    private static final Logger logger = LogManager.getLogger(HttpHandler.class);

    private static final String encoding = "UTF-8";
    private static final ObjectMapper mapper = new ObjectMapper();

    protected Config config;
    protected DatabaseManager manager;

    public HttpHandler(Config config, DatabaseManager manager) {
        this.config = config;
        this.manager = manager;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            // check basic auth
            if (!checkAuth(request)) {
                logger.warn("Unauthorized access: ip = {}", request.getRemoteAddr());
                response.setHeader("WWW-Authenticate", "Basic realm=\"RocksDB Server\"");
                writeResponse(response, new ApiResponse(401, "Unauthorized"));
                return;
            }

            // read input
            ApiRequest apiRequest;
            try {
                apiRequest = mapper.readValue(request.getInputStream(), ApiRequest.class);
            } catch (IOException e) {
                logger.error("Received invalid request from client", e);
                writeResponse(response, new ApiResponse(400, "Bad request"));
                return;
            }

            try {
                ApiResponse apiResponse;
                switch (target) {
                    case "/get":
                        apiResponse = doGet(apiRequest);
                        break;
                    case "/put":
                        apiResponse = doPut(apiRequest);
                        break;
                    case "/delete":
                        apiResponse = doDelete(apiRequest);
                        break;
                    case "/create":
                        apiResponse = doCreate(apiRequest);
                        break;
                    case "/drop":
                        apiResponse = doDrop(apiRequest);
                        break;
                    case "/stats":
                        apiResponse = doStats(apiRequest);
                        break;
                    default:
                        writeResponse(response, new ApiResponse(404, "Not found"));
                        return;
                }
                writeResponse(response, apiResponse);
            } catch (Exception e) {
                logger.error("Internal error: request = {}", apiRequest, e);
                writeResponse(response, new ApiResponse(500, "Internal error: " + e.getMessage()));
            }
        } finally {
            baseRequest.setHandled(true);
        }
    }

    /**
     * Check if the request has bee authorized.
     */
    protected boolean checkAuth(HttpServletRequest request) {
        if (!config.getAuth().isEnabled()) {
            return true;
        }

        try {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Basic ")) {
                byte[] decoded = Base64.getDecoder().decode(authorization.substring(6));
                String[] tokens = new String(decoded, encoding).split(":");

                if (config.getAuth().getUsername().equals(tokens[0])
                        && config.getAuth().getPassword().equals(tokens[1])) {
                    return true;
                }
            }
        } catch (Exception e) {
            // do nothing
        }

        return false;
    }

    /**
     * Writes an API response.
     *
     * @param response    the HTTP response
     * @param apiResponse the API response
     * @throws IOException
     */
    protected void writeResponse(HttpServletResponse response, ApiResponse apiResponse) throws IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(apiResponse.getCode());
        mapper.writeValue(response.getOutputStream(), apiResponse);
    }

    /**
     * handles a <code>get</code> request.
     */
    protected ApiResponse doGet(ApiRequest request) throws RocksDBException {
        String name = request.getName();
        List<byte[]> keys = decodeBase64(request.getKeys());

        List<byte[]> values = new ArrayList<>();
        RocksDB db = manager.open(name);
        for (byte[] key : keys) {
            values.add(db.get(key));
        }

        return new ApiResponse(200, "OK", encodeBase64(values));
    }

    /**
     * handles a <code>put</code> request.
     */
    protected ApiResponse doPut(ApiRequest request) throws RocksDBException {
        String name = request.getName();
        List<byte[]> keys = decodeBase64(request.getKeys());
        List<byte[]> values = decodeBase64(request.getValues());
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("# of keys and values don't match");
        }

        RocksDB db = manager.open(name);
        for (int i = 0; i < keys.size(); i++) {
            if (values.get(i) == null) {
                db.delete(keys.get(i));
            } else {
                db.put(keys.get(i), values.get(i));
            }
        }

        return new ApiResponse(200, "OK");
    }

    /**
     * handles a <code>delete</code> request.
     */
    protected ApiResponse doDelete(ApiRequest request) throws RocksDBException {
        String name = request.getName();
        List<byte[]> keys = decodeBase64(request.getKeys());

        RocksDB db = manager.open(name);
        for (byte[] key : keys) {
            db.delete(key);
        }

        return new ApiResponse(200, "OK");
    }

    /**
     * handles a <code>create</code> request.
     */
    protected ApiResponse doCreate(ApiRequest request) throws RocksDBException {
        String name = request.getName();

        manager.open(name);

        return new ApiResponse(200, "OK");
    }

    /**
     * handles a <code>drop</code> request.
     */
    protected ApiResponse doDrop(ApiRequest request) throws RocksDBException, IOException {
        String name = request.getName();

        manager.drop(name);

        return new ApiResponse(200, "OK");
    }

    /**
     * handles a <code>stats</code> request.
     */
    protected ApiResponse doStats(ApiRequest request) throws RocksDBException, IOException {
        String name = request.getName();

        RocksDB db = manager.open(name);
        String stats = db.getProperty("rocksdb.stats");

        return new ApiResponse(200, "OK", stats);
    }

    protected List<byte[]> decodeBase64(List<String> src) {
        if (src == null) {
            return Collections.emptyList();
        }

        return src.stream()
                .map(s -> s == null ? null : Base64.getDecoder().decode(s))
                .collect(Collectors.toList());
    }

    protected List<String> encodeBase64(List<byte[]> src) {
        if (src == null) {
            return Collections.emptyList();
        }

        return src.stream()
                .map(s -> s == null ? null : Base64.getEncoder().encodeToString(s))
                .collect(Collectors.toList());
    }
}
