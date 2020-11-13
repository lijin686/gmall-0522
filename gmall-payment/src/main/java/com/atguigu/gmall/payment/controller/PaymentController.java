package com.atguigu.gmall.payment.controller;


import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken")String orderToken, Model model){
        OrderEntity orderEntity = this.paymentService.queryOrderByToken(orderToken);
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if(orderEntity == null || orderEntity.getUserId() != userId || orderEntity.getStatus() != 0){
            throw new OrderException("非法参数");
        }
        model.addAttribute("orderEntity",orderEntity);
        return "pay";
    }

    @GetMapping("pay/ok")
    public String payOk(){
        System.out.println("hello");
        return "paysuccess";
    }

    @PostMapping("pay/success")
    @ResponseBody
    public String paySuccess(PayAsyncVo payAsyncVo){
        System.out.println("异步回调成功");
        // 1 验签
        Boolean signature = this.alipayTemplate.checkSignature(payAsyncVo);
        if(!signature){
            return "failure";
        }
        // 2 校验业务参数
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        String payId = payAsyncVo.getPassback_params();
        //获取数据库中的对账信息
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryById(payId);
        if(!StringUtils.equals(app_id,alipayTemplate.getApp_id()) || !StringUtils.equals(out_trade_no,paymentInfoEntity.getOutTradeNo())
            || paymentInfoEntity.getTotalAmount().compareTo(new BigDecimal(total_amount)) != 0)
        {
            return "failure";
        }
        // 3 校验支付状态码
        if(!StringUtils.equals(payAsyncVo.getTrade_status(),"TRADE_SUCCESS")){
            return "failure";
        }
        // 4 更新对账表
        if(this.paymentService.update(payAsyncVo) == 1){
            // 5 发送消息给oms更新订单状态，订单状态修改成功后减库存
            this.rabbitTemplate.convertAndSend("order_exchange","order.success",out_trade_no);
        }


        // 6 响应数据，告诉支付宝响应成功
        return "success";
    }

    @GetMapping("alipay.html")
    @ResponseBody
    public Object toAliPay(@RequestParam("orderToken")String orderToken){
        OrderEntity orderEntity = this.paymentService.queryOrderByToken(orderToken);
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if(orderEntity == null || orderEntity.getUserId() != userId || orderEntity.getStatus() != 0){
            throw new OrderException("非法参数");
        }
        try {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount("0.01");
            payVo.setSubject("谷粒商城支付订单");
            payVo.setPassback_params(null);

            //生成支付对账记录
            Long id = this.paymentService.savePaymentInfo(orderEntity);
            payVo.setPassback_params(id.toString());

            return this.alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

}








