package com.atguigu.gmall.oms.listener;


import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_DISABLE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.disable"}
    ))
    public void disableOrder(String orderToken, Channel channel, Message message) throws IOException {

        // 如果订单状态更新为无效订单成功，发送消息给wms解锁库存
        this.orderMapper.updateStatus(orderToken, 0, 5);

        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","stock.unlock",orderToken);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


    @RabbitListener(queues = "ORDER_DEAD_QUEUE")
    public void close(String orderToken,Channel channel,Message message) throws IOException {
        if(orderMapper.updateStatus(orderToken,0,4)==1){
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","stock.unlock",orderToken);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //订单支付成功，
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_SUCCESS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.success"}
    ))
    public void successOrder(String orderToken,Channel channel,Message message) throws IOException {

        //发送消息给wms减库存
        if(this.orderMapper.updateStatus(orderToken,0,1) == 1){
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","stock.minnus",orderToken);
        }

        //TODO：发送消息给用户添加积分

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}










