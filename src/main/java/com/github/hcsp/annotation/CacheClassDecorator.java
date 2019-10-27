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
                .subclass(klass)
                .method(ElementMatchers.isAnnotatedWith(Cache.class))
                .intercept(MethodDelegation.to(CacheInterceptor.class))
                .make()
                .load(klass.getClassLoader())
                .getLoaded();
    }

    public static class CacheKey {
      Method method;
      Object[] args;
      Object thisObject;

      CacheKey(Method method, Object[] args, Object thisObject) {
        this.method = method;
        this.args = args;
        this.thisObject = thisObject;
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

        if (!Objects.equals(method, cacheKey.method)) {
          return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(args, cacheKey.args)) {
          return false;
        }
        return Objects.equals(thisObject, cacheKey.thisObject);
      }

      @Override
      public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(args);
        result = 31 * result + (thisObject != null ? thisObject.hashCode() : 0);
        return result;
      }
    }

    public static class CacheValue {
      Object value;
      long time;

      CacheValue(Object value, long time) {
        this.value = value;
        this.time = time;
      }

    }
    public static class CacheInterceptor {
        static ConcurrentHashMap<CacheKey, CacheValue> cacheStore = new ConcurrentHashMap<>();
        // 可以用在返回值、参数上，提示ByteBuddy禁用严格的类型检查
        @RuntimeType
        public static Object cache(
                @SuperCall Callable<Object> zuper,
                @AllArguments Object[] args,
                @This Object thisObject,
                @Origin Method method) throws Exception {
          CacheKey key = new CacheKey(method, args, thisObject);
          long cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
            if (cacheStore.containsKey(key)) {
              CacheValue value = cacheStore.get(key);
              if (System.currentTimeMillis() - value.time > cacheSeconds * 1000) {
                return invokeRealMethodAndPutIntoCache(zuper, key);
              } else {
                return value.value;
              }
            } else {
              return invokeRealMethodAndPutIntoCache(zuper, key);
            }
        }

        public static Object invokeRealMethodAndPutIntoCache(
                @SuperCall Callable<Object> zuper,
                CacheKey key) throws Exception {
          // 使用@SuperCall批注，甚至可以从动态类外部执行方法的超级实现的调用
          Object result = zuper.call();
          CacheValue value = new CacheValue(result, System.currentTimeMillis());
          cacheStore.put(key, value);
          return result;
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
