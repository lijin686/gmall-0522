package com.atguigu.gmall.wms.controller;

import java.util.List;

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

import com.atguigu.gmall.wms.entity.WmsWareOrderBillEntity;
import com.atguigu.gmall.wms.service.WmsWareOrderBillService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 库存工作单
 *
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
@Api(tags = "库存工作单 管理")
@RestController
@RequestMapping("wms/wmswareorderbill")
public class WmsWareOrderBillController {

    @Autowired
    private WmsWareOrderBillService wmsWareOrderBillService;

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryWmsWareOrderBillByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = wmsWareOrderBillService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<WmsWareOrderBillEntity> queryWmsWareOrderBillById(@PathVariable("id") Long id){
		WmsWareOrderBillEntity wmsWareOrderBill = wmsWareOrderBillService.getById(id);

        return ResponseVo.ok(wmsWareOrderBill);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody WmsWareOrderBillEntity wmsWareOrderBill){
		wmsWareOrderBillService.save(wmsWareOrderBill);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody WmsWareOrderBillEntity wmsWareOrderBill){
		wmsWareOrderBillService.updateById(wmsWareOrderBill);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		wmsWareOrderBillService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
