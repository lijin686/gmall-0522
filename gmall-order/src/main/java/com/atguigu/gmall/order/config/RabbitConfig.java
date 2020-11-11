package com.atguigu.gmall.order.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        //判断消息是否到达交换机，不管有没有到达，该方法都会执行
        this.rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause)->{
            if(!ack){
                log.error("消息没有到达交换机，原因"+cause);
            }else{
                log.info("消息成功到达交换机");
            }
        });

        //判断消息是否到达队列，只有消息没有到达队列，该方法才会执行
        this.rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey)->{
            log.error("消息没有到达队列，交换机：{}，路由：{}，消息内容：{}",exchange,routingKey,message);
        });

    }
}












