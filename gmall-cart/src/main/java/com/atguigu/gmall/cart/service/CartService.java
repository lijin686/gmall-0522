package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private CartAsyncService asyncService;

    private static final String KEY_PREFIX = "cart:info:";

    private static final String PRICE_PREFIX = "cart:price:";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void addCart(Cart cart) {

        // 1 获取登录信息
        String userId = getUserId();

        String key = KEY_PREFIX+userId;
        BoundHashOperations<String,Object,Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 2 判断该用户的购物车中是否已经包含了该skuId一样的数据，如果包含了就更新数量，否则的话就新增商品

        //获取要加入购物车的skuId和销售数量
        String skuIdString = cart.getSkuId().toString();
        System.out.println("skuString"+skuIdString);
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
            this.asyncService.updateCartByUserIdAndSkuId(userId,cart);
            //this.cartMapper.update(cart,new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuIdString));

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
            //保存到数据库
            this.asyncService.saveCart(userId,cart);
            //this.cartMapper.insert(cart);
            //缓存实时价格
            if(skuEntity !=null){
                this.redisTemplate.opsForValue().set(PRICE_PREFIX+skuIdString,skuEntity.getPrice().toString());
            }

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

    // 查询购物车
    public List<Cart> queryCart() {
        // 获取userKey 查询未登录的购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unloginKey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(unloginKey);
        List<Object> cartValues = hashOps.values();
        List<Cart> unloginCart = null;
        if(!CollectionUtils.isEmpty(cartValues)){
            unloginCart = cartValues.stream().map(cartValue -> {
                try {
                    Cart cart = MAPPER.readValue(cartValue.toString(), Cart.class);
                    return cart;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                return null;

            }).collect(Collectors.toList());
        }

        //获取userId,判断是否登录，如果未登录直接返回未登录时的购物车
        Long userId = userInfo.getUserId();
        //如果userId为空表示没有登录，直接返回未登录购物车
        if(userId == null){
            return unloginCart;
        }

        // 如果用户已经登录了，就判断有没有未登录的购物车，有则合并
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        //如果有未登录时的购物车
        if(!CollectionUtils.isEmpty(unloginCart)){
            unloginCart.forEach(cart -> {
                //把未登录时的购物车合并到已登录的购物车里时，要先判断已登录购物车中是否
                //已经存在了该商品，如果存在，更新数量，如果不存在，新增商品
                if(loginHashOps.hasKey(cart.getSkuId().toString())){
                    BigDecimal count = cart.getCount();
                    //获取已登录购物车,进行序列化
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart= JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));

                } else {
                    // 如果不存在，新增购物车记录
                    cart.setUserId(userId.toString());
                    asyncService.saveCart(userId.toString(), cart);
                }

                //写回redis
                loginHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });
        }


        //删除未登录的购物车,redis和mysql中都要删除
        this.redisTemplate.delete(unloginKey);
        this.asyncService.deleteCartByUserId(userKey);

        //获取登录状态下的购物车
        List<Object> loginValues = loginHashOps.values();
        if(!CollectionUtils.isEmpty(loginValues)){
            return loginValues.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(),Cart.class);
                System.out.println(cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        return null;

    }

    //更新商品数量
    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        //更改商品数量之前首先要判断购物车中是否存在这个商品
        if(hashOps.hasKey(cart.getSkuId().toString())){
            BigDecimal count = cart.getCount();
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            //更新数量
            cart.setCount(count);


            //写回redis
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            //写回mysql
            this.asyncService.updateCartByUserIdAndSkuId(userId,cart);
        }
    }

    //用户点击商品前面的选中按钮
    public void updateStatus(Cart cart) {

        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        //先判断有没有这个商品
        if(hashOps.hasKey(cart.getSkuId().toString())){
            Boolean cartCheck = cart.getCheck();
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            //更新商品状态
            cart.setCheck(cartCheck);

            //写回redis
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            //写回mysql
            this.asyncService.updateCartByUserIdAndSkuId(userId,cart);
        }

    }

    //根据skuId删除购物车中的商品
    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        //判断该商品是否存在，如果存在就删除
        if(hashOps.hasKey(skuId.toString())){
            //删除redis
            hashOps.delete(skuId.toString());
            //删除mysql
            this.asyncService.deleteCartByUserIdAndSkuId(userId,skuId);
        }
    }

    //根据userId来查询购物车中选中的商品
    public List<Cart> queryCheackedCartByUserId(Long userId) {
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        List<Object> values = hashOps.values();
        if(!CollectionUtils.isEmpty(values)){
            return values.stream().map(value -> JSON.parseObject(value.toString(),Cart.class)).filter(Cart::getCheck).collect(Collectors.toList());
        }
        return null;

    }
}



















