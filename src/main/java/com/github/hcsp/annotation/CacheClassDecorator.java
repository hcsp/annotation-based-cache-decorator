package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
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
        Class<? extends T> loaded = new ByteBuddy() // 子类化
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
        return (Class<T>) loaded;
    }

    private static class CacheKey {
        private Object thisObj;
        private String methodName;
        private Object[] arguments;

        CacheKey(Object thisObj, String methodName, Object[] arguments) {
            this.thisObj = thisObj;
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
            CacheKey cacheKey = (CacheKey) o;
            return thisObj.equals(cacheKey.thisObj) &&
                    methodName.equals(cacheKey.methodName) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObj, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
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

    public static class CacheAdvisor {
        private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap(); // 保证线程安全

        // 调用一个被拦截的方法，参数即是调用的方法,调用的方法的对象和传入方法参数等等都可以获取到
        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> superCall,
                @Origin Method method,
                @This Object thisObj,
                @AllArguments Object[] arguments) throws Exception {
            // if 缓存中已经存在本次调用的结果，返回缓存的结果
            // else 执行真正的方法调用，并且将方法调用的结果放入缓存，并返回方法调用的结果
            CacheKey cacheKey = new CacheKey(thisObj, method.getName(), arguments);
            final CacheValue resultExistingInCache = cache.get(cacheKey);

            if (resultExistingInCache != null) {
                // 这个缓存的结果是什么时候生成的？
                // 判断缓存是否过期
                if (catchExpires(resultExistingInCache, method)) {
                    return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
                } else {
                    return resultExistingInCache.value;
                }

            } else {
                // 执行真正方法调用时可以通过反射 method.invoke(thisObj,arguments)
                // byteBuddy提供了更好的方法 @SuperCall
                // Object 真正的方法调用的结果 = superCall.call()
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            }
        }

        private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall,
                                                              CacheKey cacheKey) throws Exception {
            Object realMethodInvocationResult = superCall.call();
            cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
            return realMethodInvocationResult;
        }

        private static boolean catchExpires(CacheValue cacheValue, Method method) {
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
