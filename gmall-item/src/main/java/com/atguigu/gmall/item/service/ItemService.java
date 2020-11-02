package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 1 查询sku相关信息
        ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
        SkuEntity skuEntity = skuEntityResponseVo.getData();
        if(skuEntity != null){
            itemVo.setSkuId(skuEntity.getId());
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            itemVo.setWeight(skuEntity.getWeight());
        }

        // 2 查询分类信息
        ResponseVo<List<CategoryEntity>> categoryEntitiesByCid3 = this.pmsClient.queryCategoryEntitiesByCid3(skuEntity.getCatagoryId());
        List<CategoryEntity> categoryEntities = categoryEntitiesByCid3.getData();
        itemVo.setCategories(categoryEntities);

        // 3 查询品牌信息
        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
        BrandEntity brandEntity = brandEntityResponseVo.getData();
        if(brandEntity != null){
            itemVo.setBrandId(brandEntity.getId());
            itemVo.setBrandName(brandEntity.getName());
        }

        // 4 查询spu相关信息
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if(spuEntity != null){
            itemVo.setSpuId(spuEntity.getId());
            itemVo.setSpuName(spuEntity.getName());

        }

        // 5 查询sku图片列表
        ResponseVo<List<SkuImagesEntity>> imagesBySkuId = this.pmsClient.queryImagesBySkuId(skuId);
        List<SkuImagesEntity> imagesEntityList = imagesBySkuId.getData();
        itemVo.setImages(imagesEntityList);

        // 6 查询sku营销信息
        ResponseVo<List<ItemSaleVo>> itemSalesBySkuId = this.smsClient.queryItemSalesBySkuId(skuId);
        List<ItemSaleVo> itemSaleVoList = itemSalesBySkuId.getData();
        itemVo.setSales(itemSaleVoList);

        // 7 查询库存信息
        ResponseVo<List<WmsWareSkuEntity>> wareSkusBySkuId = this.wmsClient.queryWareSkusBySkuId(skuId);
        List<WmsWareSkuEntity> wareSkuEntityList = wareSkusBySkuId.getData();
        if(!CollectionUtils.isEmpty(wareSkuEntityList)){
            itemVo.setStore(wareSkuEntityList.stream().anyMatch(wmsWareSkuEntity -> wmsWareSkuEntity.getStock()-wmsWareSkuEntity.getStockLocked()>0));
        }

        // 8 查询spu所有的营销属性
        ResponseVo<List<SaleAttrValueVo>> saleAttrValueBySpuId = this.pmsClient.querySaleAttrValueBySpuId(skuEntity.getSpuId());
        List<SaleAttrValueVo> saleAttrValueVoList = saleAttrValueBySpuId.getData();
        itemVo.setSaleAttrs(saleAttrValueVoList);

        // 9 查询sku的销售属性
        ResponseVo<List<SkuAttrValueEntity>> saleAttrValueBySkuId = this.pmsClient.querySaleAttrValueBySkuId(skuId);
        List<SkuAttrValueEntity> skuAttrValueEntityList = saleAttrValueBySkuId.getData();
        if(!CollectionUtils.isEmpty(skuAttrValueEntityList)){
            itemVo.setSaleAttr(skuAttrValueEntityList.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId,SkuAttrValueEntity::getAttrValue)));
        }

        // 10 查询销售属性组合和skuId的映射关系
        ResponseVo<String> mappingSaleAttrValueBySpuId = this.pmsClient.querySkuIdMappingSaleAttrValueBySpuId(skuEntity.getSpuId());
        String json = mappingSaleAttrValueBySpuId.getData();
        itemVo.setSkuJsons(json);

        // 11 查询商品描述信息
        ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
        SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
        if(spuDescEntity != null){
            String[] urls = StringUtils.split(spuDescEntity.getDecript(), ",");
            itemVo.setSpuImages(Arrays.asList(urls));
        }

        // 12 查询组及组下的规格参数
        ResponseVo<List<ItemGroupVo>> groupWithAttrByCidAndSpuIdAndSkuId = this.pmsClient.queryGroupWithAttrByCidAndSpuIdAndSkuId(skuEntity.getCatagoryId(), skuEntity.getSpuId(), skuId);
        List<ItemGroupVo> itemGroupVoList = groupWithAttrByCidAndSpuIdAndSkuId.getData();
        itemVo.setGroups(itemGroupVoList);

        return itemVo;
    }
}














