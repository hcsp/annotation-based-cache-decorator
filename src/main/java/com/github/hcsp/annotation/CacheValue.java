package com.github.hcsp.annotation;

public class CacheValue {
    private Object value;
    private long cacheTime;

    public CacheValue(Object cacheKey, long cacheTime) {
        this.value = cacheKey;
        this.cacheTime = cacheTime;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
    }
}
