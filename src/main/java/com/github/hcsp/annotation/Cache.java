package com.github.hcsp.annotation;

public @interface Cache {
    // 标记缓存的时长（秒），默认60s
    int cacheSeconds() default 60;
}
