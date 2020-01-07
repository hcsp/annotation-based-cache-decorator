package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import com.github.hcsp.annotation.CacheClassDecorator.*;

public class ServiceInterceptor {
    private static Map<CacheKey, CacheValue> cacheMap = new HashMap<>();

    @RuntimeType
    public static Object cache(
            @SuperCall Callable zuper,
            @This Object thisObject,
            @Origin Method method,
            @AllArguments Object[] parameters
    )
            throws Exception {
        final CacheClassDecorator.CacheKey cacheKey = new CacheClassDecorator.CacheKey(thisObject, method.getName(), parameters);
        CacheClassDecorator.CacheValue cacheResult = cacheMap.get(cacheKey);

        if (cacheResult != null) {
            if (isCacheExpired(method, cacheResult)) {
                return callRealMethodAndCached(zuper, cacheKey);
            }
            return cacheResult.getValue();
        } else {
            return callRealMethodAndCached(zuper, cacheKey);
        }
    }

    private static boolean isCacheExpired(Method method, CacheClassDecorator.CacheValue cacheResult) {
        long currtime = System.currentTimeMillis();
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        long cacheGenerateTime = cacheResult.getGenerateTime();
        return currtime - cacheGenerateTime > cacheSeconds * 1000;
    }

    private static Object callRealMethodAndCached(@SuperCall Callable zuper, CacheClassDecorator.CacheKey cacheKey) throws Exception {
        Object realMethodResult = zuper.call();
        cacheMap.put(cacheKey, new CacheClassDecorator.CacheValue(realMethodResult, System.currentTimeMillis()));
        return realMethodResult;
    }
}
