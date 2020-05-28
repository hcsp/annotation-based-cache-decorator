package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.Arrays;
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
                .intercept(MethodDelegation.to(CacheAdivsor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    private static class CacheKey {
        private Object thisObject;
        private String methodName;
        private Object[] arogument;

        private CacheKey(Object thisObject, String methodName, Object[] arogument) {
            this.thisObject = thisObject;
            this.methodName = methodName;
            this.arogument = arogument;
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
            return Objects.equals(thisObject, cacheKey.thisObject)
                    && Objects.equals(methodName, cacheKey.methodName)
                    && Arrays.equals(arogument, cacheKey.arogument);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObject, methodName);
            result = 31 * result + Arrays.hashCode(arogument);
            return result;
        }
    }

    private static class CacheValue {
        private Object value;
        private long time;

        CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public static class CacheAdivsor {
        private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> superCall,
                @Origin Method method,
                @This Object thisObject,
                @AllArguments Object[] arguments) throws Exception {
            System.currentTimeMillis();
            CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);

            final CacheValue resultExistingIncache = cache.get(cacheKey);

            if (resultExistingIncache != null) {
                //缓存结果的生成时间


                if (cacheExpire(resultExistingIncache, method)) {
                    //缓存过期
                    return invokeRealMethodAndPutCache(superCall, cacheKey);
                } else {
                    return resultExistingIncache.value;
                }
            } else {
                return invokeRealMethodAndPutCache(superCall, cacheKey);
            }
        }

        private static Object invokeRealMethodAndPutCache(@SuperCall Callable<Object> superCall,
                                                          CacheKey cacheKey) throws Exception {

            Object realMethodInvokecationResult = superCall.call();
            cache.put(cacheKey, new CacheValue(realMethodInvokecationResult, System.currentTimeMillis()));
            return realMethodInvokecationResult;
        }

        private static boolean cacheExpire(CacheValue cacheValue, Method method) {
            long time = cacheValue.time;

            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            return System.currentTimeMillis() - time > cacheSeconds * 1000;
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
