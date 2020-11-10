package com.atguigu.gmall.wms.controller;

import java.util.List;

import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import com.atguigu.gmall.wms.service.WmsWareSkuService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 商品库存
 *
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
@Api(tags = "商品库存 管理")
@RestController
@RequestMapping("wms/waresku")
public class WmsWareSkuController {

    @Autowired
    private WmsWareSkuService wmsWareSkuService;

    @PostMapping("check/lock/{orderToken}")
    public ResponseVo<List<SkuLockVo>> checkAndLock(@RequestBody List<SkuLockVo> lockVos,@PathVariable("orderToken")String orderToken){
        List<SkuLockVo> skuLockVos = this.wmsWareSkuService.checkAndLock(lockVos,orderToken);
        return ResponseVo.ok(skuLockVos);
    }

    @GetMapping("sku/{skuId}")
    public ResponseVo<List<WmsWareSkuEntity>> queryWareSkuBySkuId(@PathVariable("skuId")Long skuId){
        List<WmsWareSkuEntity> skuEntities = this.wmsWareSkuService.list(new QueryWrapper<WmsWareSkuEntity>().eq("sku_id", skuId));
        return ResponseVo.ok(skuEntities);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryWmsWareSkuByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = wmsWareSkuService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<WmsWareSkuEntity> queryWmsWareSkuById(@PathVariable("id") Long id){
		WmsWareSkuEntity wmsWareSku = wmsWareSkuService.getById(id);

        return ResponseVo.ok(wmsWareSku);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody WmsWareSkuEntity wmsWareSku){
		wmsWareSkuService.save(wmsWareSku);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody WmsWareSkuEntity wmsWareSku){
		wmsWareSkuService.updateById(wmsWareSku);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		wmsWareSkuService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
