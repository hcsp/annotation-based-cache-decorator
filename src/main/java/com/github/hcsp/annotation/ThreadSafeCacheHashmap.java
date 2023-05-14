package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadSafeCacheHashmap {
    public static class CacheKey {
        private Object thisObject;
        private String methodName;
        private Object[] arguments;

        public CacheKey(Object thisObject, String methodName, Object[] arguments) {
            this.thisObject = thisObject;
            this.methodName = methodName;
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
            return Objects.equals(thisObject, cacheKey.thisObject) && Objects.equals(methodName, cacheKey.methodName) && Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        @SuppressWarnings({"checkstyle:NeedBraces"})
        public int hashCode() {
            int result = Objects.hash(thisObject, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    public static class CacheValue {
        private Object value;

        public CacheValue(Object value, long timeStamp) {
            this.value = value;
            this.timeStamp = timeStamp;
        }

        private long timeStamp;

        public Object getValue() {
            return value;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

    }

    public ConcurrentHashMap<CacheKey, CacheValue> cacheMap;

    public ThreadSafeCacheHashmap() {
        this.cacheMap = new ConcurrentHashMap();
    }
}
