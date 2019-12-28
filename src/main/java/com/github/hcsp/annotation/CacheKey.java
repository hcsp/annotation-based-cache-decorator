package com.github.hcsp.annotation;

import java.util.Arrays;
import java.util.Objects;

/**
 * @ClassName CacheKey
 * @Description
 * @Author 25127
 * @Date 2019/12/28 19:25
 * @Email jie.wang13@hand-china.com
 **/
public class CacheKey {
    private Object thisObject;
    private String methodName;
    private Object[] arguments;

    public CacheKey(Object thisObject, String methodName, Object[] arguments) {
        this.thisObject = thisObject;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    public Object getThisObject() {
        return thisObject;
    }

    public void setThisObject(Object thisObject) {
        this.thisObject = thisObject;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
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
        return Objects.equals(thisObject, cacheKey.thisObject) &&
                Objects.equals(methodName, cacheKey.methodName) &&
                Arrays.equals(arguments, cacheKey.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(thisObject, methodName);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
