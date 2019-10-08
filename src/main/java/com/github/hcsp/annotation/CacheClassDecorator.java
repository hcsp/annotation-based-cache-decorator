package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheClassDecorator {
    // 将传入的服务类Class进行增强
    // 使得返回一个具有如下功能的Class：
    // 如果某个方法标注了@Cache注解，则返回值能够被自动缓存注解所指定的时长
    // 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次
    // 它实际上只被调用一次，第二次的结果直接从缓存中获取
    // 注意，缓存的实现需要是线程安全的
    public static <T> Class<T> decorate(Class<T> klass) {
        return (Class<T>) new ByteBuddy()
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheInterceptor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }


    public static class CacheInterceptor {

        private static final Map<String, CacheResult> CACHE = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object intercept(@SuperCall Callable<Object> superCall, @Origin Method method, @AllArguments Object[] args) throws Exception {
            // construct the key of cache
            StringBuilder stringBuilder = new StringBuilder();
            String key = stringBuilder.append(method.getDeclaringClass().getName())
                    .append('-')
                    .append(method.getName())
                    .append('-')
                    .append(Arrays.toString(args)).toString();
            // retrieve the value of @Cache
            Cache cache = method.getAnnotation(Cache.class);
            int cacheTime = cache.cacheSeconds();
            // retrieve from cache
            CacheResult cacheResult = CACHE.get(key);
            Object res;
            if (null == cacheResult || System.currentTimeMillis() > cacheResult.getStoreTime() + cacheTime * 1000) {
                res = superCall.call();
                CACHE.put(key, new CacheResult(res, System.currentTimeMillis()));
            } else {
                res = cacheResult.getVal();
            }
            return res;
        }
    }

    private static class CacheResult {

        private Object val;

        private long storeTime;

        CacheResult(Object val, long storeTime) {
            this.val = val;
            this.storeTime = storeTime;
        }

        CacheResult() {
        }

        Object getVal() {
            return val;
        }

        public void setVal(Object val) {
            this.val = val;
        }

        public long getStoreTime() {
            return storeTime;
        }

        public void setStoreTime(long storeTime) {
            this.storeTime = storeTime;
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
