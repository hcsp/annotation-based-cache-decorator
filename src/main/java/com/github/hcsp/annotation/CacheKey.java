package com.github.hcsp.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class CacheKey {
    private final Method method;
    private final Object obj;
    private final Object[] arguments;

    public CacheKey(Method method, Object obj, Object[] arguments) {
        this.method = method;
        this.obj = obj;
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
        return Objects.equals(method, cacheKey.method) &&
                Objects.equals(obj, cacheKey.obj) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(method, obj);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
