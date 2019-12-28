package com.github.hcsp.annotation;

/**
 * @ClassName CacheValue
 * @Description
 * @Author 25127
 * @Date 2019/12/28 19:25
 * @Email jie.wang13@hand-china.com
 **/
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
