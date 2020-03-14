package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
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

    public static class CacheInterceptor {
        private static ConcurrentHashMap<CacheKey, CacheValue> cacheMap = new ConcurrentHashMap<>();

        @RuntimeType
        public static java.lang.Object cache(@SuperCall Callable<Object> call,
                                             @Origin Method method,
                                             @This Object obj,
                                             @AllArguments java.lang.Object[] arguments) {
            CacheKey cacheKey = new CacheKey(method, obj, arguments);
            if (isExistInCache(cacheKey)) {
                CacheValue cacheValue = cacheMap.get(cacheKey);
                int timeout = method.getAnnotation(Cache.class).cacheSeconds();
                return isCacheTimeout(cacheValue, timeout) ?
                        getValuesFromService(method, obj, arguments, call, cacheKey) :
                        cacheValue.getValue();
            } else {
                return getValuesFromService(method, obj, arguments, call, cacheKey);
            }
        }

        private static boolean isExistInCache(CacheKey cacheKey) {
            return cacheMap.containsKey(cacheKey);
        }

        private static boolean isCacheTimeout(CacheValue cacheValue, int timeout) {
            return System.currentTimeMillis() - cacheValue.getCacheTime() > timeout * 1000;
        }

        private static Object getValuesFromService(Method method, Object obj, Object[] arguments, Callable<Object> call, CacheKey cacheKey) {
            try {
                Object returnValue = call.call();
//                Object returnValue = method.invoke(obj, arguments);
                cacheMap.put(cacheKey, new CacheValue(returnValue, System.currentTimeMillis()));
                return returnValue;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
