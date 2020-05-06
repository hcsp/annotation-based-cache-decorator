package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @desc
 * @auther zhangsht
 * @date 2020/5/6 10:13
 */
public class CacheAdvisor {

    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> supperCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments
    ) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);

        CacheValue resultExistingInCache = cache.get(cacheKey);

        if (resultExistingInCache != null) {
            if (cacheExpires(resultExistingInCache, method)) {
                return invokeRealMethodAndPutIntoCache(supperCall, cacheKey);
            } else {
                return resultExistingInCache.getValue();
            }
        } else {
            return invokeRealMethodAndPutIntoCache(supperCall, cacheKey);
        }
    }

    private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> supperCall, CacheKey cacheKey) throws Exception {
        Object result = supperCall.call();
        cache.put(cacheKey, new CacheValue(result, System.currentTimeMillis()));
        return result;
    }

    private static boolean cacheExpires(CacheValue cacheValue, Method method) {
        long time = cacheValue.getTime();
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds() * 1000;
        return System.currentTimeMillis() - time > cacheSeconds;
    }

}
