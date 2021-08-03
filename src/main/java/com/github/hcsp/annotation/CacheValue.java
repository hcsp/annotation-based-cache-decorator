package com.github.hcsp.annotation;

public class CacheValue {
    private final long queryTime;
    private final Object result;

    public CacheValue(long queryTime, Object result) {
        this.queryTime = queryTime;
        this.result = result;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public Object getResult() {
        return result;
    }
}
