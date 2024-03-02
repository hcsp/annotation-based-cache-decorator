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
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(QueryWithCache.class))
                .make()
                .load(DataService.class.getClassLoader())
                .getLoaded();
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

    public static class QueryWithCache {
        private static final ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object queryCache(
                @SuperCall Callable<Object> superCall,
                @This Object thisObject,
                @Origin Method method,
                @AllArguments Object[] arguments
        ) throws Exception {
            //根据拿到的对象,方法名,参数判断缓存中是否有
            CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
            CacheValue cacheQueryResult = cache.get(cacheKey);
            if (cacheQueryResult != null) {
                //缓存中有,但需要判断是否超时了
                if (cacheExpires(method, cacheQueryResult)) {
                    //如果超时了就要进行真正查询,并保存到cache
                    return realQueryAndPutIntoCache(superCall, cacheKey);
                }
                return cacheQueryResult.callResult;
            } else {
                //缓存中没有,则直接执行真正查询,并保存到cache内
                return realQueryAndPutIntoCache(superCall, cacheKey);
            }
        }

        private static Object realQueryAndPutIntoCache(Callable<Object> superCall, CacheKey cacheKey) throws Exception {
            Object superCallResult = superCall.call();
            cache.put(cacheKey, new CacheValue(superCallResult));
            return superCallResult;
        }

        private static boolean cacheExpires(Method method, CacheValue cacheQueryResult) {
            Cache anno = method.getAnnotation(Cache.class);
            return System.currentTimeMillis() - cacheQueryResult.createdTime > anno.cacheSeconds() * 1000L;
        }

        private static class CacheKey {
            private final Object thisObject;
            private final String methodName;
            private final Object[] arguments;

            @Override
            public boolean equals(Object object) {
                if (this == object) {
                    return true;
                }
                if (object == null || getClass() != object.getClass()) {
                    return false;
                }
                CacheKey cacheKey = (CacheKey) object;
                return Objects.equals(thisObject, cacheKey.thisObject) && Objects.equals(methodName, cacheKey.methodName) && Arrays.equals(arguments, cacheKey.arguments);
            }

            @Override
            public int hashCode() {
                int result = Objects.hash(thisObject, methodName);
                result = 31 * result + Arrays.hashCode(arguments);
                return result;
            }

            CacheKey(Object thisObject, String methodName, Object[] arguments) {
                this.thisObject = thisObject;
                this.methodName = methodName;
                this.arguments = arguments;
            }
        }

        private static class CacheValue {
            private final Object callResult;
            private final long createdTime;

            CacheValue(Object superCallResult) {
                this.callResult = superCallResult;
                this.createdTime = System.currentTimeMillis();
            }
        }
    }
}
