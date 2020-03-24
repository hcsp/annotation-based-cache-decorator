package com.github.hcsp.annotation;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataService {
    /**
     * 根据数据ID查询一列数据，有缓存。
     *
     * @param id 数据ID
     * @return 查询到的数据列表
     */
    @Cache(cacheSeconds = 2)
    public List<Object> queryData(int id) {
        // 模拟一个查询操作
        Random random = new Random();
        int size = random.nextInt(10) + 10;
        return IntStream.range(0, size)
                .mapToObj(i -> random.nextInt(10))
                .collect(Collectors.toList());
    }

    /**
     * 根据数据ID查询一列数据，无缓存。
     *
     * @param id 数据ID
     * @return 查询到的数据列表
     */
    public List<Object> queryDataWithoutCache(int id) {
        // 模拟一个查询操作
        Random random = new Random();
        int size = random.nextInt(10) + 1;
        return IntStream.range(0, size)
                .mapToObj(i -> random.nextBoolean())
                .collect(Collectors.toList());
    }
}
