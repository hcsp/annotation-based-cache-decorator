package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yaohengfeng
 * @version 1.0
 * @date 2020/7/29 12:14
 */
public class CacheAdvisor {

    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
        final CacheValue cacheValue = cache.get(cacheKey);
        if (cacheValue != null) {
            if (isOverCachetSeconds(cacheValue, method)) {
                return invokeRealMethodAndPutInCache(superCall, cacheKey);
            }
            return cacheValue.getObject();
        } else {
            return invokeRealMethodAndPutInCache(superCall, cacheKey);
        }
    }


    //第一次或者缓存时间过后执行的真实的方法过程
    private static Object invokeRealMethodAndPutInCache(Callable<Object> superCall, CacheKey cacheKey) throws Exception {
        Object realMethodInvocationResult = superCall.call();
        cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
        return realMethodInvocationResult;
    }
    /**
    * @Description 判断缓存时间
    * @Param [cacheValue, method]
    * @return boolean
    * @Author yaohengfeng
    * @Date 2020/7/29
    */
    private static boolean isOverCachetSeconds(CacheValue cacheValue, Method method) {
        long time = cacheValue.getTime();
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - time > cacheSeconds * 1000;
    }
}
