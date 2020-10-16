package com.atguigu.gmall.wms.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WmsWareSkuMapper;
import com.atguigu.gmall.wms.entity.WmsWareSkuEntity;
import com.atguigu.gmall.wms.service.WmsWareSkuService;


@Service("wmsWareSkuService")
public class WmsWareSkuServiceImpl extends ServiceImpl<WmsWareSkuMapper, WmsWareSkuEntity> implements WmsWareSkuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WmsWareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WmsWareSkuEntity>()
        );

        return new PageResultVo(page);
    }

}