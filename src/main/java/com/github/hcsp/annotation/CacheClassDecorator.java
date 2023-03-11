package com.github.hcsp.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

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
        private Object thisObject;
        private String methodName;
        private Object[] arguments;

        public CacheKey(Object thisObject, String methodName, Object[] arguments) {
            this.thisObject = thisObject;
            this.methodName = methodName;
            this.arguments = arguments;
        }

        // Map key 必须遵循 equal & hashCode 约定
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((thisObject == null) ? 0 : thisObject.hashCode());
            result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
            result = prime * result + Arrays.deepHashCode(arguments);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            CacheKey key = (CacheKey) obj;

            return Objects.equals(thisObject, key.thisObject)
                    && Objects.equals(methodName, key.methodName)
                    && Arrays.equals(arguments, key.arguments);
        }
    }

    public static class CacheAdvisor {

        private static ConcurrentHashMap<CacheKey, Object> cache = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> superCall,
                @Origin Method method,
                @This Object thisObject,
                @AllArguments Object[] arguments) throws Exception {
            CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
            final Object resultExistingInCache = cache.get(cacheKey);
            // 如果缓存中存在结果，就返回缓存中的结果
            if (Objects.nonNull(resultExistingInCache)) {
                return resultExistingInCache;
            }
            Object realMethodInvocationResult = superCall.call();
            cache.put(cacheKey, realMethodInvocationResult);
            // 否则 执行真正的方法调用，并且把方法调用的结果放入缓存中，返回调用结果
            return realMethodInvocationResult;
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
