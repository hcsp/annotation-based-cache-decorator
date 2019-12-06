package com.github.hcsp.annotation;


import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    private static ConcurrentHashMap<CacheKey, CacheValue> cacheMap = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object thisObj,
            @AllArguments Object[] arguments) throws Exception {

        final CacheKey key = new CacheKey(method.getName(), thisObj, arguments);
        CacheValue cachedValue = cacheMap.get(key);

        if (cachedValue != null && !timeExpire(cachedValue, method)) {
            return cachedValue.getValue();
        } else {
            Object result = superCall.call();
            cacheMap.put(key, new CacheValue(result, System.currentTimeMillis()));
            return result;
        }
    }

    private static boolean timeExpire(CacheValue cachedValue, Method method) {
        return (System.currentTimeMillis() - cachedValue.getTime()) / 1000.0 > method.getAnnotation(Cache.class).cacheSeconds();
    }
}
