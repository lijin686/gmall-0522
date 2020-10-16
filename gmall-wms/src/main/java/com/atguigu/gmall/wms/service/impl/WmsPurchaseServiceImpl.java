package com.atguigu.gmall.wms.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WmsPurchaseMapper;
import com.atguigu.gmall.wms.entity.WmsPurchaseEntity;
import com.atguigu.gmall.wms.service.WmsPurchaseService;


@Service("wmsPurchaseService")
public class WmsPurchaseServiceImpl extends ServiceImpl<WmsPurchaseMapper, WmsPurchaseEntity> implements WmsPurchaseService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WmsPurchaseEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WmsPurchaseEntity>()
        );

        return new PageResultVo(page);
    }

}