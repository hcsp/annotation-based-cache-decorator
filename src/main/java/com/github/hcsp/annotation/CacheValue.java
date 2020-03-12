package com.github.hcsp.annotation;

public class CacheValue {
    private long createdAt;
    private Object data;

    public CacheValue(long createdAt, Object data) {
        this.createdAt = createdAt;
        this.data = data;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
