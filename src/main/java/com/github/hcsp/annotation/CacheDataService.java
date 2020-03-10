package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class CacheDataService {
    public static class CacheValue {
        List<Object> list;
        Long time;

        public CacheValue(List list, Long time) {
            this.list = list;
            this.time = time;
        }
    }

    static Map<CacheKey, CacheValue> cache = new HashMap<>();

    @RuntimeType
    public static List<Object> cacheQueryData(@Origin Method method,
                                              @This Object object,
                                              @AllArguments Object[] argument,
                                              @SuperCall Callable zuper) throws Exception {
        CacheKey cacheKey = new CacheKey(method, object, argument);
        if (cache.get(cacheKey) != null) {
            if (System.currentTimeMillis() - cache.get(cacheKey).time > 2 * 1000) {
                addResultToCache(zuper, cacheKey);
            }
        } else {
            addResultToCache(zuper, cacheKey);
        }
        return cache.get(cacheKey).list;
    }

    private static void addResultToCache(@SuperCall Callable zuper, CacheKey cacheKey) throws Exception {
        List<Object> result = (List<Object>) zuper.call();
        CacheValue cacheValue = new CacheValue(result, System.currentTimeMillis());
        cache.put(cacheKey, cacheValue);
    }
}


