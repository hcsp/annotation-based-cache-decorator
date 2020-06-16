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
    /*
     * 将传入的服务类Class进行增强 使得返回一个具有如下功能的Class： 如果某个方法标注了@Cache注解， 则返回值能够被自动缓存注解所指定的时长
     * 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次 它实际上只被调用一次， 第二次的结果直接从缓存中获取 注意，缓存的实现需要是线程安全的
     **/
    @SuppressWarnings("unchecked")
    public static <T> Class<T> decorate(Class<T> klass) {
        return (Class<T>) new ByteBuddy().subclass(klass).method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheHelper.class)).make().load(klass.getClassLoader()).getLoaded();
    }

    public static class CacheHelper {
        private static ConcurrentHashMap<CacheKey, CacheResult> hashMap = new ConcurrentHashMap<>();

        @RuntimeType
        public static Object cache(@SuperCall Callable<Object> superCall, @Origin Method method, @This Object object,
                @AllArguments Object[] arguments) throws Exception {
            CacheKey cachekey = new CacheKey(object, method.getName(), arguments);
            CacheResult result = hashMap.get(cachekey);
            if (result != null) {
                if (isValidCache(method, result)) {
                    return result.queryResult;
                }
                return newQueryResult(superCall, cachekey);
            } else {
                return newQueryResult(superCall, cachekey);
            }
        }

        private static Object newQueryResult(Callable<Object> superCall, CacheKey cachekey) throws Exception {
            Object queryResult = superCall.call();
            long time = System.currentTimeMillis();
            hashMap.put(cachekey, new CacheResult(queryResult, time));
            return queryResult;
        }

        private static boolean isValidCache(Method method, CacheResult result) {
            return (System.currentTimeMillis() - result.time) < method.getAnnotation(Cache.class).cacheSeconds() * 1000;
        }
    }

    static class CacheKey {
        private Object object;
        private String methodName;
        private Object[] arguments;

        CacheKey(Object object, String methodName, Object[] arguments) {
            super();
            this.object = object;
            this.methodName = methodName;
            this.arguments = arguments;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.deepHashCode(arguments);
            result = prime * result + Objects.hash(methodName, object);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (getClass() != obj.getClass()) {
                return false;
            }

            CacheKey other = (CacheKey) obj;
            return Arrays.deepEquals(arguments, other.arguments) && Objects.equals(methodName, other.methodName)
                    && Objects.equals(object, other.object);
        }
    }

    static class CacheResult {
        private Object queryResult;
        private long time;

        CacheResult(Object queryResult, long time) {
            this.queryResult = queryResult;
            this.time = time;
        }
    }

    public static void main(String[] args) throws Exception {
        DataService dataService = decorate(DataService.class).getConstructor().newInstance();

        /*
         * 有缓存的查询：只有第一次执行了真正的查询操作，第二次从缓存中获取
         */

        System.out.println(dataService.queryData(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryData(1));

        // 无缓存的查询：两次都执行了真正的查询操作
        System.out.println(dataService.queryDataWithoutCache(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryDataWithoutCache(1));
    }
}
