package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(@SuperCall Callable<Object> superCall,
                               @Origin Method method,
                               @This Object thisObject,
                               @AllArguments Object[] arguments) throws Exception {

        CacheKey cacheKey = new CacheKey(method.getName(), thisObject, arguments);
        int expiredTime = method.getAnnotation(Cache.class).cacheSeconds();
        CacheValue cacheValue = cache.get(cacheKey);
        if (!cacheExpire(cacheValue, expiredTime)) {
            return cache.get(cacheKey).getObject();
        }
        Object realValue = superCall.call();
        cache.put(cacheKey, new CacheValue(realValue, System.currentTimeMillis()));
        return realValue;

    }

    private static boolean cacheExpire(CacheValue cacheValue, int expiredTime) {
        if (cacheValue == null) {
            return true;
        }
        return System.currentTimeMillis() - cacheValue.getCreateTime() > expiredTime * 1000;
    }
}
