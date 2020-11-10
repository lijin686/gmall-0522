package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;

import java.util.List;

/**
 * 商品库存
 *
 * @author lijin
 * @email 201251671@qq.com
 * @date 2020-10-14 19:45:48
 */
public interface WmsWareSkuService extends IService<WmsWareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken);
}

