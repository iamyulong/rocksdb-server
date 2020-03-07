package com.ranksays.rocksdb.server;


public class ApiResponse {

    private Integer code;
    private String message;
    private Object body;

    public ApiResponse() {

    }

    public ApiResponse(Integer code, String message) {
        this(code, message, null);
    }

    public ApiResponse(Integer code, String message, Object body) {
        this.code = code;
        this.message = message;
        this.body = body;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getBody() {
        return body;
    }
}
