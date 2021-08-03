package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheInterceptor {
    private static final ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    public static Object getCacheDataOrRequery(
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @AllArguments Object[] arguments,
            @This Object thisObject
    ) {
        try {
            CacheKey cacheKey = new CacheKey(method.getName(), arguments, thisObject);
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            CacheValue resultExistingInCache = cache.get(cacheKey);
            if (resultExistingInCache != null && System.currentTimeMillis() - resultExistingInCache.getQueryTime() < cacheSeconds * 1000L) {
                return resultExistingInCache.getResult();
            }

            Object realMethodInvocationResult = superCall.call();
            cache.put(cacheKey, new CacheValue(System.currentTimeMillis(), realMethodInvocationResult));
            return realMethodInvocationResult;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
