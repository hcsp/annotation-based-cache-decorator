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
    public static <T> Class<T> decorate(Class<T> klass) throws IllegalAccessException, InstantiationException {
        return (Class) new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    private static class CacheKey {
        private String method;
        private Object object;
        private Object[] argument;

        CacheKey(String method, Object object, Object[] argument) {
            this.method = method;
            this.object = object;
            this.argument = argument;
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
                    Objects.equals(object, cacheKey.object) &&
                    Arrays.equals(argument, cacheKey.argument);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(method, object);
            result = 31 * result + Arrays.hashCode(argument);
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
        private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(
                //调用父类的方法
                @SuperCall Callable<Object> superCall,
                //当前正在被调用的方法
                @Origin Method method,
                //实体方法的对象
                @This Object object,
                //所有的参数
                @AllArguments Object[] arguments) throws Exception {
            //获取缓存的结果
            CacheKey cacheKey = new CacheKey(method.getName(), object, arguments);
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            final CacheValue obj = cache.get(cacheKey);

            if (obj != null) {
                long time = cache.get(cacheKey).time;
                if (cacheIsExpired(time, cacheSeconds)) {
                    //缓存过期
                    return putNewValueIntoCache(superCall, cacheKey);
                } else {
                    return obj.value;
                }
            } else {
                return putNewValueIntoCache(superCall, cacheKey);
            }
        }

        private static boolean cacheIsExpired(long time, long cacheSeconds) {
            return ((System.currentTimeMillis() - time) > cacheSeconds * 1000);
        }

        private static Object putNewValueIntoCache(@SuperCall Callable<Object> superCall, CacheKey cacheKey) throws Exception {
            Object realResult = superCall.call();
            CacheValue cacheValue = new CacheValue(realResult, System.currentTimeMillis());
            cache.put(cacheKey, cacheValue);
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
