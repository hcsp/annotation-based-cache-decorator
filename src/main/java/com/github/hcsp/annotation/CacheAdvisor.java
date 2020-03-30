package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object enhance(
            @SuperCall Callable callable,
            @Origin Method method,
            @This Object object,
            @AllArguments Object[] arguments
    ) throws Exception {
        CacheValue result;
        final CacheKey cacheKey = new CacheKey(method, object, arguments);
        long cacheTime = method.getAnnotation(Cache.class).cacheSeconds();
        if ((result = cache.get(cacheKey)) != null && System.currentTimeMillis() - result.getTime() <= cacheTime * 1000) {
            return result.getValue();
        } else {
            result = new CacheValue();
            result.setValue(callable.call());
            result.setTime(System.currentTimeMillis());
            cache.put(cacheKey, result);
            return result.getValue();
        }
    }

    private static class CacheKey {
        private Method method;
        private Object object;
        private Object[] args;

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
                    Arrays.equals(args, cacheKey.args);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(method, object);
            result = 31 * result + Arrays.hashCode(args);
            return result;
        }

        CacheKey(Method method, Object object, Object[] args) {
            this.method = method;
            this.object = object;
            this.args = args;
        }
    }

    private static class CacheValue {
        CacheValue() {
        }

        private long time;
        private Object value;

        CacheValue(long time, Object value) {
            this.time = time;
            this.value = value;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
