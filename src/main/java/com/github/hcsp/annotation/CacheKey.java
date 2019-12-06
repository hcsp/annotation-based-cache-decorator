package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;

public class CacheKey {
    private String methodName;
    private Object thisObj;
    private Object[] arguments;

    public CacheKey(String methodName, Object thisObj, Object[] arguments) {
        this.methodName = methodName;
        this.thisObj = thisObj;
        this.arguments = arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CacheKey cacheKey = (CacheKey) o;
        return Objects.equals(methodName, cacheKey.methodName) &&
                Objects.equals(thisObj, cacheKey.thisObj) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(methodName, thisObj);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
