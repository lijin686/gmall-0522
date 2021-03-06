package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.utils.CookieUtils;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVo loadData(Long skuId) {


        ItemVo itemVo = new ItemVo();

        CompletableFuture<SkuEntity> skuEntityCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 1 查询sku相关信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                itemVo.setSkuId(skuEntity.getId());
                itemVo.setTitle(skuEntity.getTitle());
                itemVo.setSubTitle(skuEntity.getSubtitle());
                itemVo.setPrice(skuEntity.getPrice());
                itemVo.setDefaultImage(skuEntity.getDefaultImage());
                itemVo.setWeight(skuEntity.getWeight());
            }
            return skuEntity;
        }, threadPoolExecutor);


        // 2 查询分类信息
        CompletableFuture<Void> cateCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> categoryEntitiesByCid3 = this.pmsClient.queryCategoryEntitiesByCid3(skuEntity.getCatagoryId());
            List<CategoryEntity> categoryEntities = categoryEntitiesByCid3.getData();
            if(!CollectionUtils.isEmpty(categoryEntities)){

                itemVo.setCategories(categoryEntities);
            }
        },threadPoolExecutor);

        // 3 查询品牌信息
        CompletableFuture<Void> brandCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if(brandEntity != null){
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        },threadPoolExecutor);

        // 4 查询spu相关信息
        CompletableFuture<Void> spuCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if(spuEntity != null){
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        },threadPoolExecutor);

        // 5 查询sku图片列表
        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() ->{
            ResponseVo<List<SkuImagesEntity>> imagesBySkuId = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> imagesEntityList = imagesBySkuId.getData();
            itemVo.setImages(imagesEntityList);
        },threadPoolExecutor);

        // 6 查询sku营销信息
        CompletableFuture<Void> salesCompletableFuture = CompletableFuture.runAsync(() ->{
            ResponseVo<List<ItemSaleVo>> itemSalesBySkuId = this.smsClient.queryItemSalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVoList = itemSalesBySkuId.getData();
            itemVo.setSales(itemSaleVoList);
        },threadPoolExecutor);

        // 7 查询库存信息
        CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() ->{

            ResponseVo<List<WmsWareSkuEntity>> wareSkusBySkuId = this.wmsClient.queryWareSkusBySkuId(skuId);
            List<WmsWareSkuEntity> wareSkuEntityList = wareSkusBySkuId.getData();
            if(!CollectionUtils.isEmpty(wareSkuEntityList)){
                itemVo.setStore(wareSkuEntityList.stream().anyMatch(wmsWareSkuEntity -> wmsWareSkuEntity.getStock()-wmsWareSkuEntity.getStockLocked()>0));
            }
        },threadPoolExecutor);

        // 8 查询spu所有的营销属性
        CompletableFuture<Void> saleAttrsCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<List<SaleAttrValueVo>> saleAttrValueBySpuId = this.pmsClient.querySaleAttrValueBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVoList = saleAttrValueBySpuId.getData();
            itemVo.setSaleAttrs(saleAttrValueVoList);
        },threadPoolExecutor);

        // 9 查询sku的销售属性
        CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() ->{

            ResponseVo<List<SkuAttrValueEntity>> saleAttrValueBySkuId = this.pmsClient.querySaleAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntityList = saleAttrValueBySkuId.getData();
            if(!CollectionUtils.isEmpty(skuAttrValueEntityList)){
                itemVo.setSaleAttr(skuAttrValueEntityList.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId,SkuAttrValueEntity::getAttrValue)));
            }
        },threadPoolExecutor);

        // 10 查询销售属性组合和skuId的映射关系
        CompletableFuture<Void> mappingCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<String> mappingSaleAttrValueBySpuId = this.pmsClient.querySkuIdMappingSaleAttrValueBySpuId(skuEntity.getSpuId());
            String json = mappingSaleAttrValueBySpuId.getData();
            itemVo.setSkuJsons(json);
        },threadPoolExecutor);

        // 11 查询商品描述信息
        CompletableFuture<Void> descCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if(spuDescEntity != null){
                String[] urls = StringUtils.split(spuDescEntity.getDecript(), ",");
                itemVo.setSpuImages(Arrays.asList(urls));
            }
        },threadPoolExecutor);

        // 12 查询组及组下的规格参数
        CompletableFuture<Void> groupCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<List<ItemGroupVo>> groupWithAttrByCidAndSpuIdAndSkuId = this.pmsClient.queryGroupWithAttrByCidAndSpuIdAndSkuId(skuEntity.getCatagoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVoList = groupWithAttrByCidAndSpuIdAndSkuId.getData();
            itemVo.setGroups(itemGroupVoList);
        },threadPoolExecutor);

        CompletableFuture.allOf(cateCompletableFuture, brandCompletableFuture, spuCompletableFuture,
                imageCompletableFuture, salesCompletableFuture, wareCompletableFuture, saleAttrsCompletableFuture,
                saleAttrCompletableFuture, mappingCompletableFuture, descCompletableFuture, groupCompletableFuture).join();
        return itemVo;
    }
}














