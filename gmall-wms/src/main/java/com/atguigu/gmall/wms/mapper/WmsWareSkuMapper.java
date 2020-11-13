package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
@Mapper
public interface WmsWareSkuMapper extends BaseMapper<WmsWareSkuEntity> {

    public List<WmsWareSkuEntity> check(@Param("skuId") Long skuId, @Param("count")Integer count);

    public int lock(@Param("id")Long id,@Param("count")Integer count);
    public int unlock(@Param("id")Long id,@Param("count")Integer count);
    public int minus(@Param("id")Long id,@Param("count")Integer count);

}
