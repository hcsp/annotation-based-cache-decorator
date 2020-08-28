package com.github.hcsp.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//设置在运行时保留注解
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {
    // 标记缓存的时长（秒），默认60s
    int cacheSeconds() default 60;
}
