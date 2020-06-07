package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    public static class CacheKey {
        String methodName;
        Object thisObject;
        Object[] arguments;

        public CacheKey(String methodName, Object thisObject, Object[] arguments) {
            this.methodName = methodName;
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
            return Objects.equals(methodName, cacheKey.methodName) &&
                    Objects.equals(thisObject, cacheKey.thisObject) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(methodName, thisObject);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    public static class CacheValue {
        Object value;
        Instant instant;

        public CacheValue(Object value, Instant instant) {
            this.value = value;
            this.instant = instant;
        }
    }

    static ConcurrentHashMap<CacheKey, CacheValue> cacheConcurrentHashMap = new ConcurrentHashMap<>();

    public static boolean isCacheValid(CacheValue cacheValue, int validSeconds) {
        if (cacheValue == null) {
            return false;
        }
        return cacheValue.instant.plusSeconds(validSeconds).isAfter(Instant.now());
    }

    @RuntimeType
    public static Object cache(
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments,
            @SuperCall Callable<Object> zuper
    ) throws Exception {
        CacheKey cacheKey = new CacheKey(method.getName(), thisObject, arguments);
        CacheValue cacheValue = cacheConcurrentHashMap.get(cacheKey);
        if (isCacheValid(cacheValue, method.getAnnotation(Cache.class).cacheSeconds())) {
            return cacheValue.value;
        }
        Object originalMethodResult = zuper.call();
        cacheConcurrentHashMap.put(cacheKey, new CacheValue(originalMethodResult, Instant.now()));
        return originalMethodResult;
    }
}
