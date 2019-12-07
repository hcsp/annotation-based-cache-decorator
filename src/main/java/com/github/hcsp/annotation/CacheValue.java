package com.github.hcsp.annotation;

public class CacheValue {
    private Object object;
    private long createTime;

    public CacheValue(Object object, long createTime) {
        this.object = object;
        this.createTime = createTime;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}
