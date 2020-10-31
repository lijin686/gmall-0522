package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cate:";

    @Autowired
    private GmallPmsClient pmsClient;
    public List<CategoryEntity> queryLvllCategories() {
        //查询一级分类的id
        ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoriesByPid(0l);

        return listResponseVo.getData();
    }

    //查询二级分类和三级分类
    public List<CategoryEntity> queryCategoryLvl2WithSubsById(Long pid) {

        // 查询缓存，如果命中缓存就直接返回，如果没有没有命中就先去数据库中查询，然后保存到缓存中
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX+pid);
        if(StringUtils.isNotBlank(json)){
            return JSON.parseArray(json,CategoryEntity.class);
        }
        //加分布式锁
        RLock lock = this.redissonClient.getLock("lock" + pid);
        lock.lock();

        List<CategoryEntity> categoryEntities;
        try {
            String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX+pid);
            if(StringUtils.isNotBlank(json2)){
                return JSON.parseArray(json2,CategoryEntity.class);
            }
            //如果没有命中缓存

            ResponseVo<List<CategoryEntity>> queryCategoryLvl2WithSubsById = this.pmsClient.queryCategoryLvl2WithSubsById(pid);
            categoryEntities = queryCategoryLvl2WithSubsById.getData();
        /*
            解决缓存穿透问题，当大量请求访问缓存中不存在的数据，导致mysql宕机
            解决办法，就算缓存中不存在也要进行缓存 null ,但是缓存的时间不能过长
         */
            if(CollectionUtils.isEmpty(categoryEntities)){
                this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryEntities),5, TimeUnit.MINUTES);
            }else{
                /*
                    解决缓存雪崩问题，大量缓存数据同时过期，同时又大量请求访问过期数据
                    解决办法，给缓存时间添加随机值
                 */
                this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryEntities),90+new Random().nextInt(5), TimeUnit.DAYS);

            }
            return categoryEntities;
        } finally {
            lock.unlock();
        }

    }

    //查询二级分类和三级分类(不用上面那种方法，使用自定义注解)
    @GmallCache(prefix = KEY_PREFIX, lock = "lock:", timeout = 129600, random = 7200)
    public List<CategoryEntity> queryCategoryLvl2WithSubsById2(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoryLvl2WithSubsById(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;

    }

    //测试分布式锁
    public void testLock() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        //setIfAbsent相当于 redis 中的 setnx命令，当缓存中没有 lock 这个 key的时候返回 true
        //Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", "xxxx");
        /*
            但是如果一个服务器获取到锁，还没执行下面的释放锁的代码就宕机了，导致其他服务器获取不到锁
            就会导致死锁现象。所以为了防止死锁，可以给锁设置过期时间。
            下面的代码表示3s后锁自动过期
         */
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
        if(!lock){
            Thread.sleep(20);
            this.testLock();
        }else{

            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("count");
            // 没有该值return
            if (StringUtils.isBlank(value)){
                this.redisTemplate.opsForValue().set("count", "1");
            }
            // 有值就转成成int
            int count = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("count", String.valueOf(++count));

            //释放锁
            //释放锁的时候先通过uuid判断一下是不是自己的锁，如果是自己的锁才释放
            if(StringUtils.equals(uuid,this.redisTemplate.opsForValue().get("lock"))){
                this.redisTemplate.delete("lock");
            }
        }
    }

    //测试分布式锁，使用lua脚本
    public void testLock1() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock",uuid,9l);
        if (lock) {
            // 获取锁成功，执行业务逻辑
            String countString = this.redisTemplate.opsForValue().get("count");
            if (StringUtils.isBlank(countString)) {
                this.redisTemplate.opsForValue().set("count", "1");
            }
            int count = Integer.parseInt(countString);
            this.redisTemplate.opsForValue().set("count", String.valueOf(++count));

            TimeUnit.SECONDS.sleep(60);

            //this.testSubLock(uuid);

            this.distributedLock.unlock("lock", uuid);
        }
    }

    //测试分布式锁，使用 redisson
    public void testLock2() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        // 获取锁成功，执行业务逻辑
        String countString = this.redisTemplate.opsForValue().get("count");
        if (StringUtils.isBlank(countString)) {
            this.redisTemplate.opsForValue().set("count", "1");
        }
        int count = Integer.parseInt(countString);
        this.redisTemplate.opsForValue().set("count", String.valueOf(++count));

        //TimeUnit.SECONDS.sleep(60);
        lock.unlock();

    }

    //测试闭锁
    public String testLatch() throws InterruptedException {

        RCountDownLatch latch = this.redissonClient.getCountDownLatch("countdowm");
        latch.trySetCount(6);
        latch.await();

        return "班长锁门";

    }

    public String testDown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("countdowm");
        latch.countDown();
        System.out.println(latch.getCount());
        return "出来一个人";

    }
}







