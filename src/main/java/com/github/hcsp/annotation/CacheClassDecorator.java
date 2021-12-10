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
        // 对目标子类化，然后把其中标注有cache注解的方法拎出来做一些特殊处理
        // 委托给CacheAdvisor这个类,
        // load是强行把类塞给getClassLoader,然后加载
        return (Class<T>) new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    private static class CacheKey {
        private Object thisObject;
        private String methodName;
        private Object[] arguments;

        CacheKey(Object thisObject, String methodName, Object[] arguments) {
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
            return Objects.equals(thisObject, cacheKey.thisObject) && Objects.equals(methodName, cacheKey.methodName) && Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObject, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    public static class CacheValue {
        public Object value;
        public long time;

        public CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public static class CacheAdvisor {
        private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> superCall,
                // 这个method是java的反射方法，在这个方法发生的时候，可以拿到当前正在发生的方法
                @Origin Method method,
                // 谁在调用
                @This Object thisObject,
                // 所有的参数
                @AllArguments Object[] arguments
        ) throws Exception {

//            if(缓存中已经存在本次调用的结果) {
//                return 缓存中的结果
//            } else {
//                执行真正的方法调用
//                并且把方法调用的结果放入缓存中
//                返回方法调用的结果
//            }
            System.currentTimeMillis();
            CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
            final CacheValue resultExistingInCache = cache.get(cacheKey);
            if (resultExistingInCache != null) {
                // 这个缓存的结果是什么时候生产
                if (cacheExpires(resultExistingInCache, method)) {
                    // 缓存过期了
                    return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
                } else {
                    return resultExistingInCache.value;
                }
            } else {
                // 真正调用的第一种方法，反射，这种方法不是最好的
//                method.invoke(thisObject, arguments);
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
            return System.currentTimeMillis() - time > cacheSeconds * 1000L;
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
