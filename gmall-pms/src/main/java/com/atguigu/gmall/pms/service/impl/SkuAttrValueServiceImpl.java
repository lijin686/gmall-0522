package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchSkuAttrValuesByCidAndSkuId(Long cid, Long skuId) {

        // 根据分类id查询出检索类型的规格参数
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));

        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }

        // 获取检索规格参数id
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        // 根据skuId和attrIds查询销售检索类型的规格参数和值
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
    }

    //根据spuId查询spu下所有sku的销售属性
    @Override
    public List<SaleAttrValueVo> querySaleAttrValueBySpuId(Long spuId) {

        List<SkuAttrValueEntity> skuAttrValueEntities = this.attrValueMapper.querySaleAttrValuesBySpuId(spuId);
        if(!CollectionUtils.isEmpty(skuAttrValueEntities)){

            List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
            //对同一个spu下的规格属性进行分组
            // SkuAttrValueEntity::getAttrId 就相当于 skuAttrValueEntity.getAttrId()
            Map<Long, List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
            map.forEach((attrId,attrValueEntities)->{
                SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
                saleAttrValueVo.setAttrId(attrId);
                saleAttrValueVo.setAttrName(attrValueEntities.get(0).getAttrName());
                saleAttrValueVo.setAttrValues(attrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
                saleAttrValueVos.add(saleAttrValueVo);
            });

            return saleAttrValueVos;
        }

        return null;
    }

    //根据spuId查询spu下所有sku的销售属性组合和skuId的映射关系
    @Override
    public String querySkuIdMappingSaleAttrValueBySpuId(Long spuId) {

        List<Map<String, Object>> maps = this.attrValueMapper.querySkuIdMappingSaleAttrValueBySpuId(spuId);
        //Map<String, Long> jsonMap = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values"), map -> Long.valueOf(map.get("sku_id").toString())));

        Map<String, Long> jsonMap = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long)map.get("sku_id")));

        return JSON.toJSONString(jsonMap);


    }

}









