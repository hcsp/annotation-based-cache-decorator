package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
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
                //子类化
                .subclass(klass)
                //找出带有Cache注解的方法
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
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
        private Object thisobject;
        private Object[] aruguments;

        public CacheKey(String methodName, Object thisobject, Object[] aruguments) {
            this.methodName = methodName;
            this.thisobject = thisobject;
            this.aruguments = aruguments;
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
            return Objects.equals(methodName, cacheKey.methodName) && Objects.equals(thisobject, cacheKey.thisobject) && Arrays.equals(aruguments, cacheKey.aruguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(methodName, thisobject);
            result = 31 * result + Arrays.hashCode(aruguments);
            return result;
        }
    }


    public static class CacheAdvisor {

        private static Map<CacheKey, CacheValue> cacheMap = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Integer> superCall,
                @Origin Method method,
                @This Object thisobject,
                @AllArguments Object[] aruguments) throws Exception {

            CacheKey cachekey = new CacheKey(method.getName(), thisobject, aruguments);
            CacheValue cachevalue = cacheMap.get(cachekey);


            if (cachevalue != null) {
                int defaultCacheTime = method.getAnnotation(Cache.class).cacheSeconds();
                //判断缓存是否过期
                if (cacheExpires(cachevalue, defaultCacheTime)) {
                    //过期的话调用原来的方法
                    return invokeSuperMethodAndStoreInToMap(superCall, cachekey);
                }
                //未过期则直接返回结果
                return cachevalue.value;
            } else {
                return invokeSuperMethodAndStoreInToMap(superCall, cachekey);
            }
        }

        private static boolean cacheExpires(CacheValue cachevalue, int defaultCacheTime) {
            return System.currentTimeMillis() - cachevalue.time > defaultCacheTime * 1000;
        }

        private static Object invokeSuperMethodAndStoreInToMap(@SuperCall Callable<Integer> superCall, CacheKey cachekey) throws Exception {
            Object result = superCall.call();
            cacheMap.put(cachekey, new CacheValue(result, System.currentTimeMillis()));
            return result;
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
