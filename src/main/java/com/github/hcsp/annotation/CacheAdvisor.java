package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {

    private static class CacheKey {
        private Method method;
        private Object thisObject;
        private Object[] arguments;

        CacheKey(Method method, Object thisObject, Object[] arguments) {
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
            return Objects.equals(method, cacheKey.method)
                    && Objects.equals(thisObject, cacheKey.thisObject)
                    && Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(method, thisObject);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    private static class CacheValue {
        private Object value;
        private Long time;

        CacheValue(Object value, Long time) {
            this.value = value;
            this.time = time;
        }
    }

    private static ConcurrentHashMap<CacheKey, CacheValue> map = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(@SuperCall Callable<Object> zuper,
                               @Origin Method method,
                               @This Object thisObject,
                               @AllArguments Object[] arguments
    ) throws Exception {
        CacheKey cacheKey = new CacheKey(method, thisObject, arguments);

        CacheValue cacheValue = map.get(cacheKey);

        if (cacheValue == null || isCacheKeyExpired(cacheValue, method)) {
            cacheValue = putValueIntoMapAndReturnCacheValue(cacheKey, zuper);
        }

        return cacheValue.value;
    }

    private static boolean isCacheKeyExpired(CacheValue cacheValue, Method method) {
        return (System.currentTimeMillis() - cacheValue.time) / 1000 > method.getAnnotation(Cache.class).cacheSeconds();
    }

    private static CacheValue putValueIntoMapAndReturnCacheValue(CacheKey cacheKey, Callable<Object> zuper) throws Exception {
        CacheValue value = new CacheValue(zuper.call(), System.currentTimeMillis());
        map.put(cacheKey, value);
        return value;
    }
}
