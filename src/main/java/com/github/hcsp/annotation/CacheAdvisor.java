package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    public static ConcurrentHashMap<CacheKey, CacheValue> map = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache (
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @AllArguments Object[] arguments
    ) throws Exception {
        System.out.println("为什么这里没有进来呢？");
        System.out.println(method.getName());
        CacheKey cacheKey = new CacheKey(method.getName(), arguments);
        CacheValue cacheValue = map.get(cacheKey);

        if (cacheValue == null || isOverdue(cacheValue, method)) {
            Object result = superCall.call();
            CacheValue newCacheValue = new CacheValue(result, System.currentTimeMillis());
            map.put(cacheKey, newCacheValue);
            return result;
        } else {
            return cacheValue.getValue();
        }
    }

    private static boolean isOverdue(CacheValue cacheValue, Method method) {
        long time = method.getAnnotation(Cache.class).cacheSeconds();

        return System.currentTimeMillis() - time * 1000 < cacheValue.getTime();
    }
}
