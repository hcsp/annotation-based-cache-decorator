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
    private static class CacheKey {
        private final String methodName;
        private final Object[] args;
        private final Object thisObj;

        CacheKey(String methodName, Object[] args, Object thisObj) {
            this.methodName = methodName;
            this.args = args;
            this.thisObj = thisObj;
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
            return Objects.equals(methodName, cacheKey.methodName) && Objects.deepEquals(args, cacheKey.args) && Objects.equals(thisObj, cacheKey.thisObj);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodName, Arrays.hashCode(args), thisObj);
        }
    }

    private static class CacheValue {
        private final Object data;
        private final long updateAt;

        CacheValue(Object data, long updateAt) {
            this.data = data;
            this.updateAt = updateAt;
        }

        public Object getData() {
            return data;
        }

        public long getUpdateAt() {
            return updateAt;
        }
    }

    private static final Map<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    public static <T> Class<? extends T> decorate(Class<T> klass) {
        return new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheAdvice.class))
                .make().load(klass.getClassLoader()).getLoaded();
    }

    public static class CacheAdvice {
        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> zuper,
                @This Object thisObj,
                @Origin Method method,
                @AllArguments Object[] args
        ) throws Exception {
            CacheKey cacheKey = new CacheKey(method.getName(), args, thisObj);
            if (cache.containsKey(cacheKey)) {
                CacheValue cacheValue = cache.get(cacheKey);
                if (isCacheExpires(method, cacheValue)) {
                    return callOriginAndPutResultToCache(zuper, cacheKey);
                } else {
                    return cacheValue.getData();
                }
            } else {
                return callOriginAndPutResultToCache(zuper, cacheKey);
            }
        }
    }

    private static boolean isCacheExpires(Method method, CacheValue cacheValue) {
        Cache anno = method.getAnnotation(Cache.class);
        return System.currentTimeMillis() - cacheValue.getUpdateAt() > anno.cacheSeconds() * 1000L;
    }

    private static Object callOriginAndPutResultToCache(Callable<Object> zuper, CacheKey cacheKey) throws Exception {
        Object result = zuper.call();
        CacheValue cacheValue = new CacheValue(result, System.currentTimeMillis());
        cache.put(cacheKey, cacheValue);
        return result;
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
