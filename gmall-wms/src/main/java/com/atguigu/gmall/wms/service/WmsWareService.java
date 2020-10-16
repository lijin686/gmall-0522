package com.atguigu.gmall.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WmsWareEntity;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
public interface WmsWareService extends IService<WmsWareEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

