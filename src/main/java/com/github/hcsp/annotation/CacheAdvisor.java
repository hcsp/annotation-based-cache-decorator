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
            @Origin Method method, // 哪个方法在调用
            @This Object thisObject, // 哪个对象在调用
            @AllArguments Object[] arguments, // 调用的方法参数
            @SuperCall Callable<Object> superCall
    ) throws Exception {
        final CacheKey cacheKey = new CacheKey(method.getName(), thisObject, arguments);
        CacheValue value = cache.get(cacheKey);
        // 缓存中存在调用结果，直接返回缓存中的结果；
        if (value != null) {

            if (isCacheTimeOut(value, method)) {
                // 缓存超时，重新调用原方法得到结果，将结果存入缓存，并返回；
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            } else {
                return value.getValue();
            }
        } else {
            // 缓存中不存在调用结果，调用原始方法得到结果，将结果存入缓存，并返回；
            return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
        }
    }

    private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall, CacheKey cacheKey) throws Exception {

        Object object = superCall.call();
        cache.put(cacheKey, new CacheValue(object, System.currentTimeMillis()));
        return object;
    }

    private static boolean isCacheTimeOut(CacheValue value, Method method) {
        long createTime = value.getTime();
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - createTime > cacheSeconds * 1000;
    }
}
