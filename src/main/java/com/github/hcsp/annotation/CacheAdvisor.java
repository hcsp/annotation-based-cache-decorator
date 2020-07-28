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

    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            // 子类化的父类
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
        final CacheValue cacheValue = cache.get(cacheKey);
        if (cacheValue != null) {
            if (isOverCachetSeconds(cacheValue, method)) {

                return getRealObject(superCall, cacheKey);
            }
            return cacheValue.getObject();
        }
        return getRealObject(superCall, cacheKey);
    }

    private static boolean isOverCachetSeconds(CacheValue cacheValue, Method method) {
        long time = cacheValue.getTime();
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - time > cacheSeconds * 1000;
    }

    private static Object getRealObject(Callable<Object> object, CacheKey cacheKey) throws Exception {
        Object realInvokeMethodResult = object.call();
        cache.put(cacheKey, new CacheValue(realInvokeMethodResult, System.currentTimeMillis()));
        return realInvokeMethodResult;
    }
}
