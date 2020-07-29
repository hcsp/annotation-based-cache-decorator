package com.github.hcsp.annotation;

/**
 * @author yaohengfeng
 * @version 1.0
 * @date 2020/7/29 12:40
 */
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
