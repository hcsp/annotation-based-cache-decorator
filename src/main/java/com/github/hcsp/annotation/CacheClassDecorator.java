package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheClassDecorator {
    // 将传入的服务类Class进行增强
    // 使得返回一个具有如下功能的Class：
    // 如果某个方法标注了@Cache注解，则返回值能够被自动缓存注解所指定的时长
    // 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次
    // 它实际上只被调用一次，第二次的结果直接从缓存中获取
    // 注意，缓存的实现需要是线程安全的
    @SuppressWarnings("unchecked")
    public static <T> Class<T> decorate(Class<T> klass) {
        return (Class<T>) new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheInterceptor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    public static class CacheInterceptor {
        private static ConcurrentHashMap<CacheKey, CacheValue> cacheMap = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object intercept(@SuperCall Callable<Object> superCall,
                                       @Origin Method method,
                                       @This Object target,
                                       @AllArguments Object[] arguments) throws Exception {
            CacheKey cacheKey = new CacheKey(target, method.getName(), arguments);
            CacheValue cacheValue = cacheMap.get(cacheKey);
            int cacheDuration = method.getAnnotation(Cache.class).cacheSeconds();
            if (cacheValue == null || isCacheExpired(cacheValue.cacheTime, cacheDuration)) {
                cacheValue = new CacheValue(superCall.call(), System.currentTimeMillis());
                cacheMap.put(cacheKey, cacheValue);
            }
            return cacheValue.value;
        }

        private static boolean isCacheExpired(long cacheTime, int cacheDuration) {
            long currentTime = System.currentTimeMillis();
            return (currentTime - cacheTime) >= 1000 * cacheDuration;
        }
    }

    private static class CacheKey {
        private Object target;
        private String methodName;
        private Object[] arguments;

        CacheKey(Object target, String methodName, Object[] arguments) {
            this.target = target;
            this.methodName = methodName;
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
            CacheKey cachedKey = (CacheKey) o;
            return Objects.equals(target, cachedKey.target) &&
                    Objects.equals(methodName, cachedKey.methodName) &&
                    Arrays.equals(arguments, cachedKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(target, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    private static class CacheValue {
        private Object value;
        private long cacheTime;

        CacheValue(Object value, long cacheTime) {
            this.value = value;
            this.cacheTime = cacheTime;
        }
    }

    public static void main(String[] args) throws Exception {
        DataService dataService = decorate(DataService.class).getConstructor().newInstance();

        // 有缓存的查询：只有第一次执行了真正的查询操作，第二次从缓存中获取
        System.out.println(dataService.queryData(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryData(1));

        // 无缓存的查询：两次都执行了真正的查询操作
        System.out.println(dataService.queryDataWithoutCache(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryDataWithoutCache(1));
    }
}
