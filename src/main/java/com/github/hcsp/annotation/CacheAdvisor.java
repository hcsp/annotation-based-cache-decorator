package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class CacheAdvisor {
    private static ThreadSafeCacheHashmap cache = new ThreadSafeCacheHashmap();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> zuper,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments
    ) {
        try {
            ThreadSafeCacheHashmap.CacheKey cacheKey = new ThreadSafeCacheHashmap.CacheKey(thisObject, method.getName(), arguments);
            Object hashmapValue = getHashMapValue(cacheKey, method.getAnnotation(Cache.class).cacheSeconds());
            if (hashmapValue != null) {
                return hashmapValue;
            }
            Object freshResult = zuper.call();
            upsertIntoHashMap(cacheKey, freshResult);
            return freshResult;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void upsertIntoHashMap(ThreadSafeCacheHashmap.CacheKey cacheKey, Object freshResult) {
        cache.cacheMap.put(cacheKey, new ThreadSafeCacheHashmap.CacheValue(freshResult, System.currentTimeMillis()));
    }

    private static Object getHashMapValue(ThreadSafeCacheHashmap.CacheKey cacheKey, int cacheSeconds) {
        if (cache.cacheMap.containsKey(cacheKey) && System.currentTimeMillis() - cache.cacheMap.get(cacheKey).getTimeStamp() <= cacheSeconds * 1000) {
            return cache.cacheMap.get(cacheKey).getValue();
        }
        return null;
    }
}
