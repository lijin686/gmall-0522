package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
    }


    @Test
    public void testBloom(){
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom");
        //预计统计的元素数量是20，期望的误差率为 30%， 也就是说布隆过滤器计算成功的概率只有70%
        bloomFilter.tryInit(20l,0.3);
        bloomFilter.add("1");
        bloomFilter.add("2");
        bloomFilter.add("3");
        bloomFilter.add("4");
        bloomFilter.add("5");
        bloomFilter.add("6");
        System.out.println(bloomFilter.contains("1"));
        System.out.println(bloomFilter.contains("3"));
        System.out.println(bloomFilter.contains("5"));
        System.out.println(bloomFilter.contains("6"));
        System.out.println(bloomFilter.contains("8"));
        System.out.println(bloomFilter.contains("10"));
        System.out.println(bloomFilter.contains("12"));
        System.out.println(bloomFilter.contains("13"));
        System.out.println(bloomFilter.contains("15"));
        System.out.println(bloomFilter.contains("16"));
        System.out.println(bloomFilter.contains("17"));
        System.out.println(bloomFilter.contains("19"));
        System.out.println(bloomFilter.contains("20"));
    }

}
