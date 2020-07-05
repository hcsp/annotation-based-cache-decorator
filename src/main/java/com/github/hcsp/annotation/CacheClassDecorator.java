package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;

import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;


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
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    public static class CacheKey {
        private Method method;
        private Object thisObject;
        private Object[] arguments;

        public CacheKey(Method method, Object thisObject, Object[] arguments) {
            this.method = method;
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
            return Objects.equals(method, cacheKey.method) &&
                    Objects.equals(thisObject, cacheKey.thisObject) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(method, thisObject);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    static class CacheValue {
        private Object result;
        private Long time;

        CacheValue(Object result, Long time) {
            this.result = result;
            this.time = time;
        }
    }

    public static class CacheAdvisor {
        private static final HashMap<CacheKey, CacheValue> cacheResult = new HashMap<>();

        @RuntimeType
        public static Object cache(@SuperCall Callable<List<String>> zuper,
                                   @Origin Method method,
                                   @This Object thisObject,
                                   @AllArguments Object[] arguments) throws Exception {
            // 判断有没有缓存
            CacheKey key = new CacheKey(method, thisObject, arguments);
            if (cacheResult.get(key) != null) {
                // 判断缓存是否过期
                int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
                if (System.currentTimeMillis() - cacheResult.get(key).time > cacheSeconds * 1000) {
                    return getRealResultAndCached(zuper, key);
                }
                return cacheResult.get(key).result;
            } else {
                return getRealResultAndCached(zuper, key);
            }
        }

        private static Object getRealResultAndCached(Callable<List<String>> zuper, CacheKey key) throws Exception {
            Object realResult = zuper.call();
            cacheResult.put(key, new CacheValue(realResult, System.currentTimeMillis()));
            return realResult;
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
