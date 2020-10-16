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

import com.atguigu.gmall.wms.entity.WmsPurchaseDetailEntity;
import com.atguigu.gmall.wms.service.WmsPurchaseDetailService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 
 *
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
@Api(tags = " 管理")
@RestController
@RequestMapping("wms/wmspurchasedetail")
public class WmsPurchaseDetailController {

    @Autowired
    private WmsPurchaseDetailService wmsPurchaseDetailService;

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryWmsPurchaseDetailByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = wmsPurchaseDetailService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<WmsPurchaseDetailEntity> queryWmsPurchaseDetailById(@PathVariable("id") Long id){
		WmsPurchaseDetailEntity wmsPurchaseDetail = wmsPurchaseDetailService.getById(id);

        return ResponseVo.ok(wmsPurchaseDetail);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody WmsPurchaseDetailEntity wmsPurchaseDetail){
		wmsPurchaseDetailService.save(wmsPurchaseDetail);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody WmsPurchaseDetailEntity wmsPurchaseDetail){
		wmsPurchaseDetailService.updateById(wmsPurchaseDetail);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		wmsPurchaseDetailService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
