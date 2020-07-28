package com.github.hcsp.annotation;

public class CacheValue {
    private Object object;
    private long time;

    public CacheValue(Object object, long time) {
        this.object = object;
        this.time = time;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
