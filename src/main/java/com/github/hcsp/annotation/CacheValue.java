package com.github.hcsp.annotation;

/**
 * @desc
 * @auther zhangsht
 * @date 2020/5/6 12:22
 */
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

    public void setValue(Object value) {
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
