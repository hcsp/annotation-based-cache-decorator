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
                .subclass(klass) // 动态创建一个klass的子类,因为你要扩展klass嘛,因此你肯定需要一个子类
//                .method(methodDescription -> klass.getAnnotation(Cache.class) != null) // 过滤出所有带有@Cache注解的方法
                .method(ElementMatchers.isAnnotatedWith(Cache.class)) // 这句和上面的意思一样
                .intercept(MethodDelegation.to(Target.class)) // 拦截这些方法并委托给谁处理
                .make() // 创建出这个Class对象
                .load(klass.getClassLoader())  // 让ClassLoader加载它
                .getLoaded(); // 返回加载后的Class对象
    }

    public static class Target {
        private static final ConcurrentHashMap<CacheK, CacheV> cache = new ConcurrentHashMap<>();

        @RuntimeType // 这个注解指定当发生委托时,要调用这个方法,如果你不想加这个注解,那必须保证方法的返回值和被拦截的方法返回值一致(目前我看到的是这样)
        public static Object intercept(
                @SuperCall Callable<Object> supperCall, // 既然我们这个Class是继承的klass,那么我们通过这个参数就能调用父类的方法
                @Origin Method method, // 当委托发生的时候,可以拿到当前被调用的方法(换句话说就是,调用哪个方法的时候发生了这个委托)
                @This Object thisObject, // 哪个对象调用了这个方法
                @AllArguments Object[] allArguments // 可以拿到所有的参数
        ) throws Exception {
            CacheK key = new CacheK(thisObject, method.getName(), allArguments);
            CacheV cached = cache.get(key);
            if (cached != null) {
                if (System.currentTimeMillis() - cached.getTime() > method.getAnnotation(Cache.class).cacheSeconds() * 1000L) {
                    return cacheValue(supperCall, key);
                }
                return cached.getValue();
            }
            return cacheValue(supperCall, key);
        }

        private static Object cacheValue(Callable<Object> supperCall, CacheK key) throws Exception {
            CacheV value = new CacheV(supperCall.call(), System.currentTimeMillis());
            cache.put(key, value);
            return value.getValue();
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
