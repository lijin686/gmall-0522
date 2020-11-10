package com.atguigu.gmall.order.service;


import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
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
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    private StringRedisTemplate redisTemplate;

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

        // 2 校验总价

        // 3 校验库存并锁库存

        // 4 下单

        // 5 删除购物车对应的数据
        return null;
    }
}
