package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class CacheInterceptor {
    private static Long previousTime = null;
    private static Parameter[] previousParameters;
    private static List<Object> memoryDatabase;


    public static List<Object> getCacheDataOrRequery(@SuperCall Callable<List<Object>> zuper, @Origin Method method) {
        try {
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            Parameter[] parameters = method.getParameters();
            if (!Arrays.equals(parameters, previousParameters) || null == memoryDatabase || (previousTime != null && System.currentTimeMillis() - previousTime > cacheSeconds * 1000L)) {
                System.out.println("call!!!!!");
                memoryDatabase = zuper.call();
            }

            previousParameters = parameters;
            previousTime = System.currentTimeMillis();
            return memoryDatabase;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
