package com.github.hcsp.annotation;


import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class DataServiceDecorator {

    private static final Map<CacheKey, CacheValue> CACHE_DATA = new ConcurrentHashMap<>();

    @RuntimeType public static Object invoke(@SuperCall Callable<Object> superCall, @Origin Method method,
            @This Object thisObject, @AllArguments Object[] arguments) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method, arguments);
        CacheValue cacheValue = CACHE_DATA.get(cacheKey);
        if (cacheValue != null && !isExpire(cacheValue, method)) {
            return cacheValue.getData();
        }
        return getResult(superCall, cacheKey);
    }

    private static boolean isExpire(CacheValue cacheValue, Method method) {
        return System.currentTimeMillis() - cacheValue.getExpireTimeStamp()
                > method.getAnnotation(Cache.class).cacheSeconds() * 1000;
    }

    private static Object getResult(Callable<Object> object, CacheKey cacheKey) throws Exception {
        Object result = object.call();
        CACHE_DATA.put(cacheKey, new CacheValue(System.currentTimeMillis(), result));
        return result;
    }

    static class CacheKey {

        private Object object;

        private Method method;

        private Object[] args;

        CacheKey(Object object, Method method, Object[] args) {
            this.setObject(object);
            this.setMethod(method);
            this.setArgs(args);
        }

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }

        @Override public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            CacheKey newKey = (CacheKey) obj;
            return Objects.equals(newKey.getObject(), this.getObject()) && Objects
                    .equals(newKey.getMethod(), this.getMethod()) && Arrays.equals(newKey.getArgs(), this.getArgs());
        }

        @Override public int hashCode() {
            int result = Objects.hash(this.getObject(), this.getMethod());
            result = 31 * result + Arrays.hashCode(this.getArgs());
            return result;
        }
    }

    static class CacheValue {

        Long expireTimeStamp;

        Object data;

        CacheValue(Long expireTimeStamp, Object data) {
            this.setExpireTimeStamp(expireTimeStamp);
            this.setData(data);
        }

        public Long getExpireTimeStamp() {
            return expireTimeStamp;
        }

        public void setExpireTimeStamp(Long expireTimeStamp) {
            this.expireTimeStamp = expireTimeStamp;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

}
