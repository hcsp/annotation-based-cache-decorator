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
    // 将传入的服务类Class进行增强
    // 使得返回一个具有如下功能的Class：
    // 如果某个方法标注了@Cache注解，则返回值能够被自动缓存注解所指定的时长
    // 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次
    // 它实际上只被调用一次，第二次的结果直接从缓存中获取
    // 注意，缓存的实现需要是线程安全的
    @SuppressWarnings("unchecked")
    static <T> Class<T> decorate(Class<T> klass) {
        return (Class<T>) new ByteBuddy()  //动态字节码增强
                .subclass(klass)  // 对目标klass子类化，为原有的类增加功能继承,且为类型的子类
                .method(ElementMatchers.isAnnotatedWith(Cache.class))  //对包含Cache这个注解的类进行处理
                .intercept(MethodDelegation.to(CacheAdvisor.class)) //拦截,通过委托给这个类完成
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    private static class CacheKey {
        private Object thisObject;
        private String methodName;
        private Object[] arguments; //确保这三个都一致，才认为是map的key，通过EchoHashcode完成判断是否相等

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
            if (!(o instanceof CacheKey)) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return thisObject.equals(cacheKey.thisObject)
                    && methodName.equals(cacheKey.methodName)
                    && Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObject, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
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


    public static class CacheAdvisor {  //实现缓存功能，识别是谁会被拦截

        static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap();

        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> superCall,   //调用父类的方法
                @Origin Method method,     //知道是谁在调用方法，通过反射
                @This Object thisObject,
                @AllArguments Object[] arguments) throws Exception {
            CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
            final CacheValue resultExistingInCache = cache.get(cacheKey);


            if (resultExistingInCache != null) {

                if (cacheExpires(resultExistingInCache, method)) {
                    //缓存过期了
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
            //缓存生成的时间
            long time = cacheValue.time;
            int cacheSeconds = method.getDeclaredAnnotation(Cache.class).cacheSeconds();
            return (System.currentTimeMillis() - time) > cacheSeconds * 1000;
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
