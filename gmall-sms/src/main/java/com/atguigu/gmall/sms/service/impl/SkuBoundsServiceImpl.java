package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.service.SkuFullReductionService;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.hibernate.validator.constraints.EAN;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.util.CollectionUtils;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuFullReductionMapper fullReductionMapper;

    @Autowired
    private SkuFullReductionService fullReductionService;

    @Autowired
    private SkuLadderMapper skuLadderMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public void saveSales(SkuSaleVo skuSaleVo) {

        // 1 保存商品积分设置 sms_sku_bounds
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuSaleVo,skuBoundsEntity);
        List<Integer> works = skuSaleVo.getWork();
        if(!CollectionUtils.isEmpty(works) && works.size() == 4){
            //把二进制转化为十进制 例如works=1001 转化为10进制为 9
            skuBoundsEntity.setWork(works.get(3)*8 + works.get(2)*4 + works.get(1)*2 + works.get(0));
        }
        this.save(skuBoundsEntity);

        // 2 保存商品满减(满2000减1000) sms_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        this.fullReductionMapper.insert(skuFullReductionEntity);


        // 3 保存商品阶梯价格(满几件打几折) sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        this.skuLadderMapper.insert(skuLadderEntity);

    }

    //根据skuId查询sku所有的营销信息
    @Override
    public List<ItemSaleVo> queryItemSalesBySkuId(Long skuId) {

        List<ItemSaleVo> itemSaleVos = new ArrayList<>();

        //查询积分优惠
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if(skuBoundsEntity != null){
            ItemSaleVo boundItemSaleVo = new ItemSaleVo();
            boundItemSaleVo.setType("积分");
            boundItemSaleVo.setDesc("赠送"+skuBoundsEntity.getGrowBounds()+"成长积分,赠送"+skuBoundsEntity.getBuyBounds()+"优惠积分");
            itemSaleVos.add(boundItemSaleVo);
        }


        //查询满减优惠
        SkuFullReductionEntity skuFullReductionEntity = this.fullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if(skuFullReductionEntity != null){
            ItemSaleVo reductItemSaleVo = new ItemSaleVo();
            reductItemSaleVo.setDesc("满"+skuFullReductionEntity.getFullPrice()+"减"+skuFullReductionEntity.getReducePrice());
            reductItemSaleVo.setType("满减");
            itemSaleVos.add(reductItemSaleVo);
        }
        //查询打折优惠
        SkuLadderEntity skuLadderEntity = this.skuLadderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if(skuLadderEntity != null){
            ItemSaleVo ladderItemSaleVo = new ItemSaleVo();
            ladderItemSaleVo.setType("打折");
            ladderItemSaleVo.setDesc("满"+skuLadderEntity.getFullCount()+"件，打"+skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");
            itemSaleVos.add(ladderItemSaleVo);
        }

        return itemSaleVos;

    }

}