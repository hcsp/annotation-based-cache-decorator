package com.github.hcsp.annotation;

public class CacheValue {
    private Object value;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    private long time;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public CacheValue(Object value, long time) {
        this.value = value;
        this.time = time;
    }
}
