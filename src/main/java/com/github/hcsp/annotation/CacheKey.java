package com.github.hcsp.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class CacheKey {
    Method method;
    Object object;
    Object[] arguments;

    public CacheKey(Method method, Object object, Object[] arguments) {
        this.method = method;
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
        return Objects.equals(method, cacheKey.method) &&
                Objects.equals(object, cacheKey.object) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(method, object);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
