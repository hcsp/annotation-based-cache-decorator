package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
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
                //klass子类化
                .subclass(klass)
                //筛选Cache注解的方法
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                //拦截匹配的方法,把方法委托给CacheAdvisor这个类做处理
                .intercept(MethodDelegation.to(CacheAdvisor.class))
                //创建这个子类
                .make()
                //获取klass类的类加载器
                .load(klass.getClassLoader())
                //加载子类
                .getLoaded();
    }

    public static class CacheKey {
        private String method;
        private Object thisObject;
        private Object[] arguments;

        public CacheKey(String method, Object thisObject, Object[] arguments) {
            this.method = method;
            this.thisObject = thisObject;
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
            return Objects.equals(method, cacheKey.method) &&
                    Objects.equals(thisObject, cacheKey.thisObject) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(method, thisObject);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    public static class CacheValue {
        private Object value;
        //时间是从1970年午夜0点开始
        private long time;

        public CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public static class CacheAdvisor {
        private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

        //缓存功能
        @RuntimeType //使拦截时能找到相匹配的方法并调用
        public static Object cache(
                @SuperCall Callable<Object> superCall,
                @Origin Method method,
                @This Object thisObject,
                @AllArguments Object[] arguments
        ) throws Exception {
            CacheKey cacheKey = new CacheKey(method.getName(), thisObject, arguments);

            CacheValue resultExistingInCache = cache.get(cacheKey);
            if (resultExistingInCache != null) {
                if (cacheExpires(resultExistingInCache, method)) {
                    //缓存过期,执行真正的调用
                    return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
                } else {
                    //如果缓存中已经存在本次调用的结果，就返回缓存中的结果
                    return resultExistingInCache.value;
                }
            } else {
                /**执行真正的方法调用,把方法调用的结果保存到缓存中，返回方法调用的结果 **/
                return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
            }
        }

        private static Object invokeRealMethodAndPutIntoCache(@SuperCall Callable<Object> superCall, CacheKey cacheKey) throws Exception {
            //执行父类真正的方法调用
            Object realMethodInvocationResult = superCall.call();
            //把数据放到缓存中
            cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));

            return realMethodInvocationResult;
        }

        private static boolean cacheExpires(CacheValue resultExistingInCache, Method method) {
            //缓存结果的生成时间
            long time = resultExistingInCache.time;
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            return System.currentTimeMillis() - time > cacheSeconds * 1000;
        }
    }

    public static void main(String[] args) throws Exception {
        DataService dataService = decorate(DataService.class).getConstructor().newInstance();

        // 有缓存的查询：只有第一次执行了真正的查询操作，第二次从缓存中获取
        System.out.println(dataService.queryData(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryData(1));
        Thread.sleep(1 * 1000);

        // 无缓存的查询：两次都执行了真正的查询操作
        System.out.println(dataService.queryDataWithoutCache(1));
        Thread.sleep(1 * 1000);
        System.out.println(dataService.queryDataWithoutCache(1));
    }
}
