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
    public static Object cache(
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
        final CacheValue resultExistingInCache = cache.get(cacheKey);

        if (resultExistingInCache != null) {
            if (cacheExpires(resultExistingInCache, method)) {
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            } else {
                return resultExistingInCache.value;
            }
        } else {
            return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
        }
    }

    private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall, CacheKey cacheKey) throws Exception {
        Object realMethodInvocationResult = superCall.call();
        cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
        return realMethodInvocationResult;
    }

    private static boolean cacheExpires(CacheValue cacheValue, Method method) {
        long time = cacheValue.time;
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - time > cacheSeconds * 1000;
    }

    private static class CacheKey {
        private Object thisObject;
        private String methodName;
        private Object[] arguments;

        CacheKey(Object thisObject, String methodName, Object[] arguments) {
            this.thisObject = thisObject;
            this.methodName = methodName;
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
            return Objects.equals(thisObject, cacheKey.thisObject) &&
                    Objects.equals(methodName, cacheKey.methodName) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObject, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    private static class CacheValue {
        private Object value;
        private long time;

        CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }
}
