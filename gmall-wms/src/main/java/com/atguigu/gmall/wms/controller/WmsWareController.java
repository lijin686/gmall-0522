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

import com.atguigu.gmall.wms.entity.WmsWareEntity;
import com.atguigu.gmall.wms.service.WmsWareService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 仓库信息
 *
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
@Api(tags = "仓库信息 管理")
@RestController
@RequestMapping("wms/wmsware")
public class WmsWareController {

    @Autowired
    private WmsWareService wmsWareService;

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryWmsWareByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = wmsWareService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<WmsWareEntity> queryWmsWareById(@PathVariable("id") Long id){
		WmsWareEntity wmsWare = wmsWareService.getById(id);

        return ResponseVo.ok(wmsWare);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody WmsWareEntity wmsWare){
		wmsWareService.save(wmsWare);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody WmsWareEntity wmsWare){
		wmsWareService.updateById(wmsWare);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		wmsWareService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
