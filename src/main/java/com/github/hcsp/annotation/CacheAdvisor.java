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

    private static final ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object self,
            @AllArguments Object[] arguments
    ) throws Exception {
        CacheKey cacheKey = new CacheKey(self, method.getName(), arguments);
        CacheValue cacheResult = cache.get(cacheKey);
        if (cacheResult != null) {
            long time = cacheResult.time;
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            if (cacheExpires(time, cacheSeconds)) {
                return invokeMethodPutIntoCache(superCall, cacheKey);
            } else {
                return cacheResult.value;
            }
        } else {
            return invokeMethodPutIntoCache(superCall, cacheKey);
        }
    }

    private static Object invokeMethodPutIntoCache(@SuperCall Callable<Object> superCall, CacheKey cacheKey) throws Exception {
        Object result = superCall.call();
        cache.put(cacheKey, new CacheValue(System.currentTimeMillis(), result));
        return result;
    }

    private static boolean cacheExpires(long time, int cacheSeconds) {
        return System.currentTimeMillis() - time > cacheSeconds * 1000;
    }

    private static class CacheValue {
        private final long time;
        private final Object value;

        CacheValue(long time, Object value) {
            this.time = time;
            this.value = value;
        }
    }

    private static class CacheKey {
        private final Object thisObject;
        private final String methodName;
        private final Object[] arguments;

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
}
