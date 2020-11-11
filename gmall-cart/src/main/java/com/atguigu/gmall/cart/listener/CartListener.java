package com.atguigu.gmall.cart.listener;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import javafx.beans.binding.ObjectExpression;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    private static final String PRICE_PREFIX = "cart:price:";
    private static final String KEY_PREFIX = "cart:info:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_PMS_QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {

        //如果spuId为空，消费消息返回空
        if(spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return ;
        }

        //如果不为空，获取该spuId下的所有的sku
        ResponseVo<List<SkuEntity>> listResponseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntityList = listResponseVo.getData();
        if(!CollectionUtils.isEmpty(skuEntityList)){
            skuEntityList.forEach(skuEntity -> {
                String price = this.redisTemplate.opsForValue().get(PRICE_PREFIX+skuEntity.getId());
                if(StringUtils.isNoneBlank(price)){
                    this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
                }
            });
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //获取消息队列中的内容，订单提交成功后删除购物车中的数据
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_DELETE_QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void delete(Map<String, Object> map,Channel channel,Message message) throws IOException {
        if(CollectionUtils.isEmpty(map)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return ;
        }

        String userId = map.get("userId").toString();
        String skuIds = map.get("skuIds").toString();
        List<String> skuIdList = JSON.parseArray(skuIds, String.class);

        //获取该userId下的购物车对象
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX+userId);
        //删除缓存中的购物车订单已经提交的商品
        hashOps.delete(skuIdList.toArray());
        //删除mysql中的购物车订单中已经提交的商品
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId).in("sku_id",skuIdList));
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
