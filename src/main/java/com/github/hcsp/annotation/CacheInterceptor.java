package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class CacheInterceptor {
    private static Long previousTime = null;
    private static Object[] previousArguments;
    private static List<Object> memoryDatabase;


    public static List<Object> getCacheDataOrRequery(
            @SuperCall Callable<List<Object>> zuper,
            @Origin Method method,
            @AllArguments Object[] arguments
    ) {
        try {
            int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            if (!Arrays.equals(arguments, previousArguments) || null == memoryDatabase || (previousTime != null && System.currentTimeMillis() - previousTime > cacheSeconds * 1000L)) {
                memoryDatabase = zuper.call();
            }

            previousArguments = arguments;
            previousTime = System.currentTimeMillis();
            return memoryDatabase;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
