package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;

public class CacheKey {
    private Object object;
    private String method;
    private Object[] arguments;

    public CacheKey(Object object, String method, Object[] arguments) {
        this.object = object;
        this.method = method;
        this.arguments = arguments;
    }

    public Object getObject() {
        return object;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return arguments;
    }

    // 作为Map的key要遵守equals和Hashcode约定
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CacheKey cacheKey = (CacheKey) o;
        return Objects.equals(object, cacheKey.object) &&
                Objects.equals(method, cacheKey.method) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(object, method);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
