package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;

public class CacheKey {
    private String methodName;
    private Object object;
    private Object[] arguments;

    public CacheKey(String methodName, Object object, Object[] arguments) {
        this.methodName = methodName;
        this.object = object;
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
                Objects.equals(object, cacheKey.object) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(methodName, object);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
