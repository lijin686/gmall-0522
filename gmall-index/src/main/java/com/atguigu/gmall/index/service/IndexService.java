package com.atguigu.gmall.index.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;
    public List<CategoryEntity> queryLvllCategories() {
        //查询一级分类的id
        ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.queryCategoriesByPid(0l);

        return listResponseVo.getData();
    }
}
