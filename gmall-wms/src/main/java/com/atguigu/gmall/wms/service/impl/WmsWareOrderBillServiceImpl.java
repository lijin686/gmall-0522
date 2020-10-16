package com.atguigu.gmall.wms.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WmsWareOrderBillMapper;
import com.atguigu.gmall.wms.entity.WmsWareOrderBillEntity;
import com.atguigu.gmall.wms.service.WmsWareOrderBillService;


@Service("wmsWareOrderBillService")
public class WmsWareOrderBillServiceImpl extends ServiceImpl<WmsWareOrderBillMapper, WmsWareOrderBillEntity> implements WmsWareOrderBillService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WmsWareOrderBillEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WmsWareOrderBillEntity>()
        );

        return new PageResultVo(page);
    }

}