package com.github.hcsp.annotation;

import java.util.List;

public class CacheValue {
    private List<Object> value;
    private long timeStamp;

    public CacheValue(List<Object> value, long timeStamp) {
        this.value = value;
        this.timeStamp = timeStamp;
    }

    public List<Object> getValue() {
        return value;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
