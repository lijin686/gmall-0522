package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-11-10 18:11:24
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
