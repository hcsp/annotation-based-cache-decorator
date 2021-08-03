package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;

public class CacheKey {
    private final String methodName;
    private final Object[] arguments;
    private final Object thisObject;

    public CacheKey(String methodName, Object[] arguments, Object thisObject) {
        this.methodName = methodName;
        this.arguments = arguments;
        this.thisObject = thisObject;
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
        return Objects.equals(methodName, cacheKey.methodName) && Arrays.equals(arguments, cacheKey.arguments) && Objects.equals(thisObject, cacheKey.thisObject);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(methodName, thisObject);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
