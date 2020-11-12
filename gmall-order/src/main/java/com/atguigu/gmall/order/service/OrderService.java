package com.atguigu.gmall.order.service;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.api.GmallOmsApi;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.aspectj.weaver.ast.Or;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIXE = "order:token:";



    public OrderConfirmVo confirm() {

        OrderConfirmVo confirmVo = new OrderConfirmVo();

        //获取用户地址
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressByUserId(userId);
        List<UserAddressEntity> addressEntityList = addressResponseVo.getData();
        confirmVo.setAddresses(addressEntityList);

        //获取用户购物车中选中的商品
        ResponseVo<List<Cart>> cartResponseVo = this.cartClient.queryCheackedCartByUserId(userId);
        List<Cart> cartList = cartResponseVo.getData();
        if(CollectionUtils.isEmpty(cartList)){
            throw new OrderException("购物车中没有选中的商品");
        }
        List<OrderItemVo> orderItemVos = cartList.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
            orderItemVo.setTitle(skuEntity.getTitle());
            orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
            orderItemVo.setPrice(skuEntity.getPrice());

            ResponseVo<List<WmsWareSkuEntity>> wareSkusBySkuId = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WmsWareSkuEntity> wareSkuEntityList = wareSkusBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntityList)) {
                orderItemVo.setStore(wareSkuEntityList.stream().anyMatch(wmsWareSkuEntity -> wmsWareSkuEntity.getStock() - wmsWareSkuEntity.getStockLocked() > 0));
            }
            ResponseVo<List<SkuAttrValueEntity>> attrValueBySkuId = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> attrValueEntityList = attrValueBySkuId.getData();
            orderItemVo.setSaleAttrs(attrValueEntityList);

            ResponseVo<List<ItemSaleVo>> itemSalesBySkuId = this.smsClient.queryItemSalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVoList = itemSalesBySkuId.getData();
            orderItemVo.setSales(itemSaleVoList);

            return orderItemVo;

        }).collect(Collectors.toList());
        confirmVo.setItems(orderItemVos);

        //获取用户积分
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if(userEntity != null){

            confirmVo.setBounds(userEntity.getIntegration());
        }

        //生成唯一token
        String orderToken = IdWorker.get32UUID();
        confirmVo.setOrderToken(orderToken);
        this.redisTemplate.opsForValue().set(KEY_PREFIXE+orderToken,orderToken);

        return confirmVo;

    }

    public OrderEntity submit(OrderSubmitVo submitVo) {


        // 1 防重
        String orderToken = submitVo.getOrderToken();
        if(orderToken == null){
            throw new OrderException("非法提交");
        }

        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        Boolean execute = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIXE + orderToken), orderToken);
//        this.redisTemplate.opsForValue()
        if(!execute){
            throw new OrderException("提交过快，请稍后再试");
        }

        // 2 校验总价
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("请先选择要支付的商品");
        }
        BigDecimal currentTotalPrice = items.stream().map(orderItemVo -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(orderItemVo.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                return skuEntity.getPrice().multiply(orderItemVo.getCount());
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();

        //compareTo 返回值不为0代表两个比较值不相等
        if(currentTotalPrice.compareTo(totalPrice)!=0){
            throw new OrderException("页面已经过期，请刷新重试");
        }

        // 3 校验库存并锁库存
        List<SkuLockVo> skuLockVoStream = items.stream().map(orderItemVo -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(orderItemVo.getSkuId());
            skuLockVo.setCount(orderItemVo.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuResponseVo = this.wmsClient.checkAndLock(skuLockVoStream, orderToken);
        List<SkuLockVo> skuLockVoList = skuResponseVo.getData();
        //如果不为空表示锁库存失败
        if(!CollectionUtils.isEmpty(skuLockVoList)){
            throw new OrderException(JSON.toJSONString(skuLockVoList));
        }
        //测试出现异常，定时解锁库存
        //int i = 1/0;
        // 4 下单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        OrderEntity orderEntity = null;
        try {
            ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.saveOrder(submitVo, userId);
            orderEntity = orderEntityResponseVo.getData();
        } catch (Exception e) {
            e.printStackTrace();
            //标记订单是无效订单并解锁库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.disable",orderToken);

        }

        // 5 删除购物车对应的数据，向消息队列中发送skuId 和 userId
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        Map<String,Object> map = new HashMap<>();
        map.put("userId",userId);
        map.put("skuIds",JSON.toJSONString(skuIds));
        //发送消息到消息队列
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","cart.delete",map);

        return orderEntity;
    }
}
