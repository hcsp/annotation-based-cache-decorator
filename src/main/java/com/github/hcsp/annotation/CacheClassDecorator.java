package com.github.hcsp.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheClassDecorator {
    // 将传入的服务类Class进行增强
    // 使得返回一个具有如下功能的Class：
    // 如果某个方法标注了@Cache注解，则返回值能够被自动缓存注解所指定的时长
    // 这意味着，在短时间内调用同一个服务的同一个@Cache方法两次
    // 它实际上只被调用一次，第二次的结果直接从缓存中获取
    // 注意，缓存的实现需要是线程安全的
//        public static <T> Class<T> decorate(Class<T> klass) {

        public static <T> Class<T> decorate(Class<T> klass) {
        return new ByteBuddy()
                .redefine(klass)
                .method(method -> getMethodsWithCache(klass).contains(method.getName()))
                .intercept(MethodDelegation.to(CacheInterceptor.class))
                .name(klass.getTypeName())
                .make()
                .load(CacheClassDecorator.class.getClassLoader())
                .getLoaded();
    }

    static List<String> getMethodsWithCache(Class klass) {
        return Stream.of(klass.getMethods())
                .filter(CacheClassDecorator::isAnnotationWithCache)
                .map(Method::getName)
                .collect(Collectors.toList());
    }

    private static boolean isAnnotationWithCache(Method method) {
        // 尝试通过反射获取方法上的注解
        return method.getAnnotation(Cache.class) != null;
    }

    public static class CacheInterceptor {
        public static void cache(@SuperCall Callable<Void> zuper) throws Exception {
            try {
                zuper.call();
            } finally {

            }
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
