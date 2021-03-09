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
        return (Class<T>) new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))       // 通过匹配模式拦截
                .intercept(MethodDelegation.to(CacheAdvisor.class))         // 将拦截到的方法委托给CacheAdvisor
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    public static class CacheKey {
        private final Object thisObject;
        private final String methodName;
        private final Object[] arguments;

        public CacheKey(Object thisObject, String methodName, Object[] arguments) {
            this.thisObject = thisObject;
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
            return Objects.equals(thisObject, cacheKey.thisObject) &&
                    Objects.equals(methodName, cacheKey.methodName) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObject, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    private static class CacheValue {
        private final Object value;
        private final long time;

        CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public static class CacheAdvisor {
        static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        @RuntimeType
        // 有了这个注解，byteBuddy会忽略对入参与返回值做严格类型检查。从而使得会自动进行绑定方法与被绑定方法的参数、返回值方便的转换。
        // 这个方法的参数与返回值要与目标类需要代理的方法的参数和返回值一致。且要是唯一一致的。名字无所谓起。
        public static Object cache(                         // 注入的参数们
                @SuperCall Callable<Object> superCall,      // 被代理对象的方法调用，可以使用.call()方法调用
                @Origin Method method,      // 该参数分配为对它所检测的方法或构造函数的引用，被代理方法对象
                @This Object thisObject,    // 当前动态生成的那个对象的引用，代理对象
                @AllArguments Object[] arguments) throws Exception {
            CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
            CacheValue resultExistingInCache = cache.get(cacheKey);

            if (resultExistingInCache != null) {

                if (cacheExpires(resultExistingInCache, method)) {
                    return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
                } else {
                    return resultExistingInCache.value;
                }
            } else {
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            }
        }

        private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall, CacheKey cacheKey) throws Exception {
            Object realMethodInvocationResult = superCall.call();
            cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
            return realMethodInvocationResult;
        }

        private static boolean cacheExpires(CacheValue cacheValue, Method method) {
            long time = cacheValue.time;
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();

            return (System.currentTimeMillis() - time) > cacheSeconds * 1000L;
        }

    }

    public static void main(String[] args) throws Exception {
        DataService dataService = decorate(DataService.class).getConstructor().newInstance();

        // 有缓存的查询：只有第一次执行了真正的查询操作，第二次从缓存中获取
        System.out.println(dataService.queryData(1));
        Thread.sleep(1000);
        System.out.println(dataService.queryData(1));

        // 无缓存的查询：两次都执行了真正的查询操作
        System.out.println(dataService.queryDataWithoutCache(1));
        Thread.sleep(1000);
        System.out.println(dataService.queryDataWithoutCache(1));
    }
}