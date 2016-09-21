package com.ranksays.rocksdb.server;

import org.json.JSONObject;

/**
 * API response protocol.
 * 
 */
public class Response {
	public static final int CODE_OK = 0;
	public static final int CODE_UNAUTHORIZED = 401;
	public static final int CODE_INTERNAL_ERROR = 500;
	public static final int CODE_INVALID_DB_NAME = 1001;
	public static final int CODE_INVALID_KEY = 1002;
	public static final int CODE_INVALID_VALUE = 1003;
	public static final int CODE_FAILED_TO_OPEN_DB = 1004;

	public int code;
	public String message;
	public Object results;

	public Response(int code, String message, Object results) {
		super();
		this.code = code;
		this.message = message;
		this.results = results;
	}

	public Response(int code, String message) {
		this(code, message, null);
	}

	public Response(Object result) {
		this(CODE_OK, "Success", result);
	}

	public Response() {
		this(CODE_OK, "Success", null);
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getResults() {
		return results;
	}

	public void setResults(Object results) {
		this.results = results;
	}

	@Override
	public String toString() {
		return "Response [code=" + code + ", message=" + message + ", results=" + results + "]";
	}

	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		obj.put("code", code);
		obj.put("message", message);
		obj.put("results", results);

		return obj;
	}

	public static Response fromJSON(JSONObject obj) {
		Response resp = new Response();

		resp.setCode(obj.getInt("code"));
		resp.setMessage(obj.getString("message"));
		if (!obj.isNull("results")) {
			resp.setResults(obj.get("results"));
		}

		return resp;
	}
}
