package com.github.hcsp.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class CacheKey {
    private Method method;
    private Object thisObject;
    private Object[] arguments;

    public CacheKey(Method method, Object thisObject, Object[] arguments) {
        this.method = method;
        this.thisObject = thisObject;
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
                Objects.equals(thisObject, cacheKey.thisObject) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(method, thisObject);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
