package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    private static final ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(@SuperCall Callable<Object> superCall, @Origin Method method, @This Object object, @AllArguments Object[] arguments) throws Exception {
        final CacheKey cacheKey = new CacheKey(object, arguments, method.getName());
        if (cache.containsKey(cacheKey)) {
            Cache annotation = method.getAnnotation(Cache.class);
            CacheValue cacheValue = cache.get(cacheKey);
            if (System.currentTimeMillis() - cacheValue.cachedTime > annotation.cacheSeconds() * 1000) {
                Object result = superCall.call();
                long cachedTime = System.currentTimeMillis();
                CacheValue newCacheValue = new CacheValue(result, cachedTime);
                cache.put(cacheKey, newCacheValue);
                return result;
            } else {
                return cacheValue.value;
            }
        }
        Object result = superCall.call();
        long cachedTime = System.currentTimeMillis();
        CacheValue cacheValue = new CacheValue(result, cachedTime);
        cache.put(cacheKey, cacheValue);
        return result;
    }

    private static class CacheKey {
        private final Object object;
        private final Object[] arguments;
        private final String methodName;

        CacheKey(Object object, Object[] arguments, String methodName) {
            this.object = object;
            this.arguments = arguments;
            this.methodName = methodName;
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
            return Objects.equals(object, cacheKey.object) &&
                    Arrays.equals(arguments, cacheKey.arguments) &&
                    Objects.equals(methodName, cacheKey.methodName);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(object, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    private static class CacheValue {
        private final Object value;
        private final long cachedTime;

        CacheValue(Object value, long cachedTime) {
            this.value = value;
            this.cachedTime = cachedTime;
        }
    }
}

