package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    private static ConcurrentHashMap<CacheKey, CacheValue> cacheMap = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
        final CacheValue resultExistingInCache = cacheMap.get(cacheKey);

        if (resultExistingInCache != null) {
            if (cacheExpires(resultExistingInCache, method)) {
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            } else {
                return resultExistingInCache.getValue();
            }
        } else {
            return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
        }
    }

    private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall,
                                                          CacheKey cacheKey) throws Exception {
        Object realMethodInvocationResult = superCall.call();
        cacheMap.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
        return realMethodInvocationResult;
    }

    private static boolean cacheExpires(CacheValue cacheValue, Method method) {
        long time = cacheValue.getTime();
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - time > cacheSeconds * 1000;
    }
}
