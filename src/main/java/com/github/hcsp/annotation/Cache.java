package com.github.hcsp.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)//运行时也可以通过反射被扫描
public @interface Cache {
    // 标记缓存的时长（秒），默认60s
    int cacheSeconds() default 60;
}
