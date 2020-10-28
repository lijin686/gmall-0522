package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {


    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuAttrValueService attrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    /*
        根据f分类id查询商品
     */
    @Override
    public PageResultVo querySpuByCidPage(Long cid, PageParamVo pageParamVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();

        //如果cid为0则是查询全站，如果不为0则是查询本站
        //所以如果不为0才拼查询条件
        if(cid != 0){
            wrapper.eq("category_id",cid);
        }

        //获取输入框中输入的查询的关键字
        String key = pageParamVo.getKey();

        //如果关键字为空，查询所有，如果不为空，拼查询条件
        if(StringUtils.isNoneBlank(key)){
            /*
                查询 id 或者 name 中存在 7 的数据
                SELECT * FROM `pms_spu` where category_id=225 and (id = 7 or name like "%7%")

             */
            wrapper.and(t -> t.eq("id",key).or().like("name",key));
        }

        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @Override
    @Transactional
    public void bigSave(SpuVo spu) {

        // 一 保存spu相关信息
        // 1 保存pms_spu
        spu.setCreateTime(new Date());
        spu.setUpdateTime(new Date());
        this.save(spu);
        Long spuId = spu.getId();
        // 2 保存 pms_spu_attr_value
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            //批量保存
            this.attrValueService.saveBatch( baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo,spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                spuAttrValueEntity.setSort(0);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }

        // 3 保存 spu_desc
        if (!CollectionUtils.isEmpty(spu.getSpuImages())) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spu.getSpuImages(),","));
            this.descMapper.insert(spuDescEntity);
        }

        //二 保存sku相关信息
        List<SkuVo> skus = spu.getSkus();
        skus.forEach(skuVo -> {
            // 1 保存pms_sku
            skuVo.setSpuId(spuId);
            skuVo.setCatagoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                //设置默认图片,如果有默认图片就使用该图片，如果没有就从列表中取出第一个作为默认图片
                skuVo.setDefaultImage(StringUtils.isNoneBlank(skuVo.getDefaultImage())? skuVo.getDefaultImage():images.get(0));
            }
            //保存
            this.skuMapper.insert(skuVo);
            //获取sku的id
            Long skuVoId = skuVo.getId();


            // 2 保存 pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(skuAttrValueEntity -> {
                    skuAttrValueEntity.setSkuId(skuVoId);
                    skuAttrValueEntity.setSort(0);

                });
                this.skuAttrValueService.saveBatch(saleAttrs);

            }


            // 3 保存 pms_sku_images
            if (!CollectionUtils.isEmpty(images)) {
                //批量保存
                this.imagesService.saveBatch(images.stream().map(image ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuVoId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setSort(0);
                    //如果当前图片和sku中设置的图片一样，设置图片状态为1，否则设置为0
                    if(StringUtils.equals(image,skuVo.getDefaultImage())){
                        skuImagesEntity.setDefaultStatus(1);
                    }
                    return skuImagesEntity;
                }).collect(Collectors.toList()));

            }


            // 三 保存营销信息
            //通过 feign 远程调用 gmall-sms 中的接口
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuVoId);
            this.gmallSmsClient.saveSales(skuSaleVo);

        });
        //int ii = 1/0;


        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE","item.insert",spuId);
        System.out.println("执行了吗！！");

    }

}









