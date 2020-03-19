package com.github.hcsp.annotation;

public class CacheValue {
    private Object value;
    private long time;

    public CacheValue(Object value, long time) {
        this.value = value;
        this.time = time;
    }

    public Object getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }
}
