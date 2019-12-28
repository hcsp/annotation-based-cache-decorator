package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName CacheAdvisor
 * @Description 缓存功能
 * @Author 25127
 * @Date 2019/12/28 19:22
 * @Email jie.wang13@hand-china.com
 **/
public class CacheAdvisor {

    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable<Object> superCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments
    ) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
        final CacheValue resultExistingInCache = cache.get(cacheKey);

        // key 由多个组成 => 对象
        if (resultExistingInCache != null) {
            // 这个缓存的结果是什么时候生成的？
            long time = resultExistingInCache.getTime();
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            if (System.currentTimeMillis() - time > cacheSeconds * 1000) {
                // 缓存过期了
                Object result = superCall.call();
                cache.put(cacheKey, new CacheValue(result, System.currentTimeMillis()));
                return result;
            } else {
                return resultExistingInCache.getValue();
            }
        } else {
            Object result = superCall.call();
            cache.put(new CacheKey(thisObject, method.getName(), arguments), new CacheValue(result, System.currentTimeMillis()));
            return result;
        }
    }
}
