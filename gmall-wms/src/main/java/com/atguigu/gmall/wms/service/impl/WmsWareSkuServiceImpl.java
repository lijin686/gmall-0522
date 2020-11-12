package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WmsWareSkuMapper;
import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import com.atguigu.gmall.wms.service.WmsWareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wmsWareSkuService")
public class WmsWareSkuServiceImpl extends ServiceImpl<WmsWareSkuMapper, WmsWareSkuEntity> implements WmsWareSkuService {

    @Autowired
    private WmsWareSkuMapper wmsWareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "stock:lock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WmsWareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WmsWareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {

        if(CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("没有选中商品");
        }
        lockVos.forEach(skuLockVo -> {
            //锁定库存
            this.checkLock(skuLockVo);
        });

        //只要有一个锁定失败，就解锁全部的商品
        if(lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())){
            //先获取锁定成功的列表
            List<SkuLockVo> skuLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            //解锁库存
            skuLockVos.forEach(lockVo -> {
                this.wmsWareSkuMapper.unlock(lockVo.getWareSkuId(),lockVo.getCount());
            });
            //如果锁定失败，返回一个集合用于前端展示
            return lockVos;

        }

         //缓存锁定信息
        this.redisTemplate.opsForValue().set(KEY_PREFIX+orderToken, JSON.toJSONString(lockVos));
        //防止宕机导致死锁情况，要定时解锁库存
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","stock.ttl",orderToken);
        //如果锁定成功就返回null
        return null;
    }

    //锁定库存
    public void checkLock(SkuLockVo lockVo){

        RLock fairLock = this.redissonClient.getFairLock("stock" + lockVo.getSkuId());
        fairLock.lock();

        //查库存
        List<WmsWareSkuEntity> wmsWareSkuEntities = this.wmsWareSkuMapper.check(lockVo.getSkuId(), lockVo.getCount());
        if(CollectionUtils.isEmpty(wmsWareSkuEntities)){
            lockVo.setLock(false);
            fairLock.unlock();
            return;
        }

        //锁库存
        Long id = wmsWareSkuEntities.get(0).getId();
        if(this.wmsWareSkuMapper.lock(id,lockVo.getCount())==1){
            lockVo.setLock(true);
            lockVo.setWareSkuId(id);
        }

        fairLock.unlock();
    }



}