package com.atguigu.gmall.wms.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WmsWareMapper;
import com.atguigu.gmall.wms.entity.WmsWareEntity;
import com.atguigu.gmall.wms.service.WmsWareService;


@Service("wmsWareService")
public class WmsWareServiceImpl extends ServiceImpl<WmsWareMapper, WmsWareEntity> implements WmsWareService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WmsWareEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WmsWareEntity>()
        );

        return new PageResultVo(page);
    }

}