package com.atguigu.gmall.scheduling.job;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduling.mapper.CartMapper;
import com.atguigu.gmall.scheduling.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";

    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private CartMapper cartMapper;

    @XxlJob("AsyncExceptionJobHandler")
    public ReturnT<String> asyncException(String param){

        // 读取异步执行失败的第一个用户
        BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);
        String userId = listOps.rightPop();

        while (!StringUtils.isEmpty(userId) ){
            // 先删除mysql中的购物车
            this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));

            // 再查询该用户redis中的购物车
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();

            // 判断redis中的购物车是否为空，不为空需要遍历反序列化，新增mysql记录
            if (!CollectionUtils.isEmpty(cartJsons)){
                cartJsons.forEach(cartJson -> {
                    Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                    this.cartMapper.insert(cart);
                });
            }
            // 继续读取下一个失败用户
            userId = listOps.rightPop();
        }
        return ReturnT.SUCCESS;
    }
}
