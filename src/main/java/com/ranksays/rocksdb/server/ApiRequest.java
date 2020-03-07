package com.ranksays.rocksdb.server;

import java.util.ArrayList;
import java.util.List;

public class ApiRequest {
    private String name;
    private List<String> keys = new ArrayList<>();
    private List<String> values = new ArrayList<>();

    public String getName() {
        return name;
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<String> getValues() {
        return values;
    }
}
