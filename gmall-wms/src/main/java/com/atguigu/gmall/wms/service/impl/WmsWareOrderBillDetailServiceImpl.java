package com.atguigu.gmall.wms.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WmsWareOrderBillDetailMapper;
import com.atguigu.gmall.wms.entity.WmsWareOrderBillDetailEntity;
import com.atguigu.gmall.wms.service.WmsWareOrderBillDetailService;


@Service("wmsWareOrderBillDetailService")
public class WmsWareOrderBillDetailServiceImpl extends ServiceImpl<WmsWareOrderBillDetailMapper, WmsWareOrderBillDetailEntity> implements WmsWareOrderBillDetailService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WmsWareOrderBillDetailEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WmsWareOrderBillDetailEntity>()
        );

        return new PageResultVo(page);
    }

}