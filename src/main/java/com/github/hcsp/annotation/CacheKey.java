package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;

/**
 * @desc
 * @auther zhangsht
 * @date 2020/5/6 12:03
 */
public class CacheKey {
    private Object thisObject;
    private String methodName;
    private Object[] arguments;

    public CacheKey(Object thisObject, String methodName, Object[] arguments) {
        this.thisObject = thisObject;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey cacheKey = (CacheKey) o;
        return Objects.equals(thisObject, cacheKey.thisObject) &&
                Objects.equals(methodName, cacheKey.methodName) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(thisObject, methodName);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
