package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {

    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
        @SuperCall Callable<Object> superCall,
        @Origin Method method,
        @This Object thisObject,
        @AllArguments Object[] arguments) throws Exception {

        CacheKey key = new CacheKey(method.getName(), thisObject, arguments);
        CacheValue alreadyExistInCache = cache.get(key);

        if (alreadyExistInCache != null) { // 缓存中已经存在
            // 缓存数据生成时间
            if (isCacheExpiration(alreadyExistInCache, method)) { // 缓存过期
                return invokeRealMethodThenPutIntoCache(superCall, key);
            } else { // 未过期 直接从缓存中返回
                return alreadyExistInCache.value;
            }
        } else {
            return invokeRealMethodThenPutIntoCache(superCall, key);
        }

    }

    private static Object invokeRealMethodThenPutIntoCache(@SuperCall Callable<Object> superCall, CacheKey key) throws Exception {
        Object value = superCall.call();
        long currentTime = System.currentTimeMillis();
        cache.put(key, new CacheValue(value, currentTime));
        return value;
    }

    private static boolean isCacheExpiration(CacheValue cacheValue, Method method) {
        long time = cacheValue.time;
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - time > cacheSeconds * 1000;
    }


    private static class CacheKey {
        String methodName;
        Object thisObject;
        Object[] arguments;

        CacheKey(String methodName, Object thisObject, Object[] arguments) {
            this.methodName = methodName;
            this.thisObject = thisObject;
            this.arguments = arguments;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return methodName.equals(cacheKey.methodName) &&
                thisObject.equals(cacheKey.thisObject) &&
                Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(methodName, thisObject);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    private static class CacheValue {
        Object value;
        long time;

        CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }

}
