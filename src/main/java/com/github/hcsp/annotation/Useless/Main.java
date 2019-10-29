package com.github.hcsp.annotation.Useless;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        AnnotationPractise annotationPractise = enhanceAnnotation();
        annotationPractise.selectDatabase();
        annotationPractise.queryDatabase();
    }



    private static AnnotationPractise enhanceAnnotation() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return new ByteBuddy()
                //动态的生成AnnotationPractise的子类
                .subclass(AnnotationPractise.class)
                //匹配带Log注解的方法
                .method(new FilterAnnotationWithLog())
                //将方法拦截并委托给LoggerInterceptor
                .intercept(MethodDelegation.to(LoggerInterceptor.class))
                .make()
                .load(Main.class.getClassLoader())
                .getLoaded()
                .getConstructor()
                .newInstance();
    }

    public static class FilterAnnotationWithLog implements ElementMatcher<MethodDescription> {

        @Override
        public boolean matches(MethodDescription target) {
            //获取自身所有的方法，并将其转换为流
            List<String> collect = Stream.of(AnnotationPractise.class.getDeclaredMethods())
                    //过滤出方法的注解为Log的方法
                    .filter(method -> method.getAnnotation(Log.class) != null)
                    .map(Method::getName)
                    .collect(Collectors.toList());
            System.out.println(target.getName());
            return collect.contains(target.getName());
        }
    }

    public static class LoggerInterceptor {
        public static void log(@SuperCall Callable<Void> zuper)
                throws Exception {
            System.out.println("Calling database");
            try {
                zuper.call();
            } finally {
                System.out.println("Returned from database");
            }
        }
    }

}
