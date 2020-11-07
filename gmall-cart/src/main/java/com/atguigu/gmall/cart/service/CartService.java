package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:info:";

    public void addCart(Cart cart) {

        // 1 获取登录信息
        String userId = getUserId();

        String key = KEY_PREFIX+userId;
        BoundHashOperations<String,Object,Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 2 判断该用户的购物车中是否已经包含了该skuId一样的数据，如果包含了就更新数量，否则的话就新增商品

        //获取要加入购物车的skuId和销售数量
        String skuIdString = cart.getSkuId().toString();
        BigDecimal cartCount = cart.getCount();

        // 判断购物车中是否存在该商品,如果存在就更新数量
        if(hashOps.hasKey(skuIdString)){
            //获取内层map，也就是购物车对象
            String cartJson = hashOps.get(skuIdString).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(cartCount));
            //更新redis
            hashOps.put(skuIdString,JSON.toJSONString(cart));
            //更新数据库
            this.cartMapper.update(cart,new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuIdString));

        }else{
            //如果购物车中不存在该商品，就新增
            cart.setUserId(userId);
            cart.setCheck(true);
            //设置sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if(skuEntity != null){
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
                cart.setDefaultImage(skuEntity.getDefaultImage());
            }
            //设置库存
            ResponseVo<List<WmsWareSkuEntity>> listResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WmsWareSkuEntity> wmsWareSkuEntities = listResponseVo.getData();
            System.out.println(wmsWareSkuEntities.size());
            if(!CollectionUtils.isEmpty(wmsWareSkuEntities)){
                cart.setStore(wmsWareSkuEntities.stream().anyMatch(wmsWareSkuEntity -> wmsWareSkuEntity.getStock()-wmsWareSkuEntity.getStockLocked()>0));
            }

            //设置营销
            ResponseVo<List<ItemSaleVo>> listResponseVo1 = this.smsClient.queryItemSalesBySkuId(cart.getSkuId());
            cart.setSales(JSON.toJSONString(listResponseVo1.getData()));

            //设置营销属性
            ResponseVo<List<SkuAttrValueEntity>> listResponseVo2 = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            cart.setSaleAttrs(JSON.toJSONString(listResponseVo2.getData()));

            this.cartMapper.insert(cart);

        }
        hashOps.put(skuIdString, JSON.toJSONString(cart));

    }

    public String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if(userId == null){
            return userInfo.getUserKey();
        }
        return userId.toString();
    }

    public Cart queryCartBySkuId(Long skuId) {

        //获取登录信息
        String userId = this.getUserId();
        String key = KEY_PREFIX+userId;
        BoundHashOperations<String,Object,Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 判断购物车是否存在该数据
        if(hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson,Cart.class);
        }
        throw new RuntimeException("购物车不存在该商品");
    }
}
