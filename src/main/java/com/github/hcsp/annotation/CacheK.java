package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;

public class CacheK {
    private Object object;
    private String methodName;
    private Object[] allArguments;

    public CacheK(Object object, String methodName, Object[] allArguments) {
        this.object = object;
        this.methodName = methodName;
        this.allArguments = allArguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CacheK cacheK = (CacheK) o;
        return Objects.equals(object, cacheK.object) && Objects.equals(methodName, cacheK.methodName) && Arrays.equals(allArguments, cacheK.allArguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(object, methodName);
        result = 31 * result + Arrays.hashCode(allArguments);
        return result;
    }
}
