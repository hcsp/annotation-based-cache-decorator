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
import java.util.concurrent.ConcurrentMap;

public class CacheClassDecorator {
    // 将传入的服务类Class进行增强
    // 使得返回一个具有如下功能的Class：
    // 如果某个方法标注了@Cache注解，则返回值能够被自动缓存注解所指定的时长
    // 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次
    // 它实际上只被调用一次，第二次的结果直接从缓存中获取
    // 注意，缓存的实现需要是线程安全的
    @SuppressWarnings("unchecked")
    public static <T> Class<T> decorate(Class<T> klass) {
        Class<T> loaded = (Class<T>) new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
        return loaded;
    }

    public static class CacheValue {
        private Object value;
        private long time;

        public CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public static class CacheKey {
        private String methodName;
        private Object thisObject;
        private Object[] argunments;

        public CacheKey(String methodName, Object thisObject, Object[] argunments) {
            this.methodName = methodName;
            this.thisObject = thisObject;
            this.argunments = argunments;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cashKey = (CacheKey) o;
            return methodName.equals(cashKey.methodName) &&
                    thisObject.equals(cashKey.thisObject) &&
                    Arrays.equals(argunments, cashKey.argunments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(methodName, thisObject);
            result = 31 * result + Arrays.hashCode(argunments);
            return result;
        }
    }

    /**
     * 方法增强主题
     */
    public static class CacheAdvisor {
        private static ConcurrentMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> superCall, //真正方法的调用
                @Origin Method method, //待增强的方法
                @This Object thisObject,    //调用待增强的方法的类
                @AllArguments Object[] arguments) throws Exception {    //待增强方法的参数
            CacheKey cacheKey = new CacheKey(method.getName(), thisObject, arguments);
            final CacheValue resultExistingInCache = cache.get(cacheKey);
            if (resultExistingInCache != null) {
                //缓存生成时间
                if (cacheExpires(resultExistingInCache, method)) {  //缓存过期了
                    //得到原来的查询结果
                    return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
                }
                return resultExistingInCache.value;
            } else {
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            }
        }

        /**
         * 缓存是否过期了
         *
         * @param cacheValue 存入注解时的时刻
         * @param method     注解默认的过期时间
         * @return 是否过期
         */
        public static boolean cacheExpires(CacheValue cacheValue, Method method) {
            long time = cacheValue.time;
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            return System.currentTimeMillis() - time > cacheSeconds * 1000;
        }

        /**
         * 未进行增强的方法返回的结果
         *
         * @param superCall 待增强的方法
         * @param cacheKey  缓存的key
         * @return 查询结果
         * @throws Exception
         */
        public static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall, CacheKey cacheKey) throws Exception {
            //得到原来的查询结果
            Object realMethodInvocationResult = superCall.call();
            //将结果存入缓存;
            cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
            //返回结果;
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
