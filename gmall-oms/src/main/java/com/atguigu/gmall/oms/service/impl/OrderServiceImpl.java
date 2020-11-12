package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.UmsFeignClient;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private UmsFeignClient umsFeignClient;

    @Autowired
    private OrderItemService itemService;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {

        // 1 保存订单表
        OrderEntity orderEntity = new OrderEntity();
        ResponseVo<UserEntity> userEntityResponseVo = this.umsFeignClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if(userEntity != null){
            orderEntity.setUserId(userId);
            orderEntity.setUsername(userEntity.getUsername());
        }
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(1);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());

        UserAddressEntity address = submitVo.getAddress();
        if(address != null){
            orderEntity.setReceiverAddress(address.getAddress());
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverRegion(address.getRegion());
        }
        orderEntity.setConfirmStatus(0);
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());
        this.save(orderEntity);
        Long id = orderEntity.getId();

        // 2 保存订单详情表
        List<OrderItemVo> items = submitVo.getItems();
        if(!CollectionUtils.isEmpty(items)){
            itemService.saveBatch(items.stream().map(orderItemVo -> {

                OrderItemEntity orderItemEntity = new OrderItemEntity();
                orderItemEntity.setOrderId(id);
                orderItemEntity.setOrderSn(submitVo.getOrderToken());

                //根据skuId查询sku
                ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(orderItemVo.getSkuId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if(skuEntity != null){
                    orderItemEntity.setSkuId(skuEntity.getId());
                    orderItemEntity.setSkuName(skuEntity.getName());
                    orderItemEntity.setSkuQuantity(skuEntity.getWeight());
                    orderItemEntity.setSkuPrice(skuEntity.getPrice());
                    orderItemEntity.setSkuPic(skuEntity.getDefaultImage());
                    ResponseVo<List<SkuAttrValueEntity>> listResponseVo = this.pmsClient.querySaleAttrValueBySkuId(orderItemVo.getSkuId());
                    List<SkuAttrValueEntity> attrValueEntityList = listResponseVo.getData();
                    if(!CollectionUtils.isEmpty(attrValueEntityList)){
                        orderItemEntity.setSkuAttrsVals(JSON.toJSONString(attrValueEntityList));
                    }
                    orderItemEntity.setCategoryId(skuEntity.getCatagoryId());

                    ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    if(brandEntity != null){
                        orderItemEntity.setSpuBrand(brandEntity.getName());
                    }
                    ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
                    SpuEntity spuEntity = spuEntityResponseVo.getData();
                    if(spuEntity != null){
                        orderItemEntity.setSpuId(spuEntity.getId());
                        orderItemEntity.setSpuName(spuEntity.getName());
                    }
                    ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
                    SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
                    if(spuDescEntity != null){
                        orderItemEntity.setSpuPic(spuDescEntity.getDecript());
                    }
                }

                return orderItemEntity;

            }).collect(Collectors.toList()));
        }

        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.ttl",submitVo.getOrderToken());

        return orderEntity;
    }

}