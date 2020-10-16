package com.atguigu.gmall.wms.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WmsPurchaseDetailMapper;
import com.atguigu.gmall.wms.entity.WmsPurchaseDetailEntity;
import com.atguigu.gmall.wms.service.WmsPurchaseDetailService;


@Service("wmsPurchaseDetailService")
public class WmsPurchaseDetailServiceImpl extends ServiceImpl<WmsPurchaseDetailMapper, WmsPurchaseDetailEntity> implements WmsPurchaseDetailService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WmsPurchaseDetailEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WmsPurchaseDetailEntity>()
        );

        return new PageResultVo(page);
    }

}