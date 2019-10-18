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

/**
 * @author SuyuZhuang
 * @date 2019/10/18 9:56 下午
 */
public class CachedAdvisor {

    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

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

        CacheValue(Object callResult, long currentTimeMiles) {
            this.value = callResult;
            this.time = currentTimeMiles;
        }
    }

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> supperCall,
            @Origin Method method,
            @This Object thisObj,
            @AllArguments Object[] arguments
    ) throws Exception {
        long currentTimeMiles = System.currentTimeMillis();
        CacheKey cacheKey = new CacheKey(thisObj, method.getName(), arguments);
        CacheValue cacheValue = cache.get(cacheKey);

        if (cacheValue != null) {
            if (cacheExpires(currentTimeMiles, cacheValue, method)) {
                return invokeAndPutNewResultIntoCache(supperCall, currentTimeMiles, cacheKey);
            } else {
                return cacheValue.value;
            }
        } else {
            return invokeAndPutNewResultIntoCache(supperCall, currentTimeMiles, cacheKey);
        }
    }

    private static Object invokeAndPutNewResultIntoCache(@SuperCall Callable<Object> supperCall, long currentTimeMiles, CacheKey cacheKey) throws Exception {
        Object callResult = supperCall.call();
        CacheValue newCacheValue = new CacheValue(callResult, currentTimeMiles);
        cache.put(cacheKey, newCacheValue);
        return callResult;
    }

    private static boolean cacheExpires(long currentTimeMiles, CacheValue cacheValue, Method method) {
        int cachedSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return currentTimeMiles - cacheValue.time > cachedSeconds * 1000;
    }
}
