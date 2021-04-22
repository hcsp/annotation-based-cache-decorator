package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chengpeng[OF3832]
 * @company qianmi.com
 * @date 2021-04-22
 */
public class CacheHandle {

    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(@Origin Method method,
                               @This Object thisObj,
                               @AllArguments Object[] args,
                               @SuperCall Callable<Object> superCall) throws Exception {
        CacheKey cacheKey = new CacheKey(method, thisObj, args);
        CacheValue cacheValue = getCacheValue(cacheKey, method);
        if (cacheValue != null) {
            return cacheValue.getValue();
        }
        Object result = superCall.call();
        cache.put(cacheKey, new CacheValue(result));

        return result;
    }

    private static CacheValue getCacheValue(CacheKey cacheKey, Method method) {
        CacheValue cacheValue = cache.get(cacheKey);
        if (cacheValue == null) {
            return null;
        }
        Long cacheTiming = cacheValue.getCacheTiming();
        Long now = System.currentTimeMillis();
        int cacheSeconds = method.getDeclaredAnnotation(Cache.class).cacheSeconds();
        if ((now - cacheTiming) / 1000 > cacheSeconds) {
            cache.remove(cacheKey);
            return null;
        }
        return cacheValue;
    }

    static class CacheKey {
        Method method;
        Object thisObj;
        Object[] args;

        CacheKey(Method method, Object thisObj, Object[] args) {
            this.method = method;
            this.thisObj = thisObj;
            this.args = args;
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
            return Objects.equals(method, cacheKey.method) &&
                    Objects.equals(thisObj, cacheKey.thisObj) &&
                    Arrays.equals(args, cacheKey.args);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(method, thisObj);
            result = 31 * result + Arrays.hashCode(args);
            return result;
        }
    }

    static class CacheValue {
        Object value;
        Long cacheTiming;

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Long getCacheTiming() {
            return cacheTiming;
        }

        public void setCacheTiming(Long cacheTiming) {
            this.cacheTiming = cacheTiming;
        }

        CacheValue(Object value) {
            this.value = value;
            this.cacheTiming = System.currentTimeMillis();
        }
    }

}
