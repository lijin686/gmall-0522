package com.atguigu.gmall.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WmsPurchaseDetailEntity;

import java.util.Map;

/**
 * 
 *
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
public interface WmsPurchaseDetailService extends IService<WmsPurchaseDetailEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

