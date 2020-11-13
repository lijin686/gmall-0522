package com.atguigu.gmall.payment.service;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.api.GmallOmsApi;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private GmallOmsClient omsClient;
    public OrderEntity queryOrderByToken(String orderToken) {

        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByToken(orderToken);
        OrderEntity orderEntity = orderEntityResponseVo.getData();
        return orderEntity;
    }

    public Long savePaymentInfo(OrderEntity orderEntity) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setCreateTime(new Date());
        paymentInfoEntity.setTotalAmount(new BigDecimal(0.01));
        paymentInfoEntity.setSubject("谷粒商城支付订单");
        paymentInfoEntity.setPaymentType(orderEntity.getPayType());
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());
        this.paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId();

    }

    public PaymentInfoEntity queryById(String id){
        return this.paymentInfoMapper.selectById(id);
    }


    public int update(PayAsyncVo payAsyncVo){
        PaymentInfoEntity paymentInfoEntity = this.queryById(payAsyncVo.getPassback_params());
        paymentInfoEntity.setPaymentStatus(1);
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        paymentInfoEntity.setCallbackTime(new Date());
        return this.paymentInfoMapper.updateById(paymentInfoEntity);
    }
}

















