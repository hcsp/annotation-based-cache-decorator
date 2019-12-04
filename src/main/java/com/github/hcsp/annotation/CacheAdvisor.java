package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    private static ConcurrentHashMap<CacheKey, cacheValue> myCache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable superCall,
            @Origin Method method,
            @This Object thisObject,
            @AllArguments Object[] arguments
    ) throws Exception {
        CacheKey cacheKey = new CacheKey(method.getName(), thisObject, arguments);

        cacheValue dataInCache = myCache.get(cacheKey);

        if (dataInCache != null) {


            if (cacheExpires(dataInCache, method)) {
                //缓存是否过期
                return invokeRealMethod(superCall, cacheKey);
            } else {
                return dataInCache.Value;
            }

        } else {
            return invokeRealMethod(superCall, cacheKey);

        }
    }

    private static Object invokeRealMethod(@SuperCall Callable superCall, CacheKey cacheKey) throws Exception {
        Object invokeTheRealMethodResult = superCall.call();
        myCache.put(cacheKey, new cacheValue(invokeTheRealMethodResult, System.currentTimeMillis()));
        return invokeTheRealMethodResult;
    }

    private static boolean cacheExpires(cacheValue dataInCache, Method method) {
        long createdTime = dataInCache.time;

        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        return System.currentTimeMillis() - createdTime > cacheSeconds * 1000;
    }

    static class cacheValue {
        private Object Value;
        private long time;

        cacheValue(Object value, long time) {
            Value = value;
            this.time = time;
        }
    }

    static class CacheKey {
        private String MethodName;
        private Object thisObject;
        private Object[] arguments;

        CacheKey(String methodName, Object thisObject, Object[] arguments) {
            MethodName = methodName;
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
            return Objects.equals(MethodName, cacheKey.MethodName) &&
                    Objects.equals(thisObject, cacheKey.thisObject) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(MethodName, thisObject);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

}

