package com.atguigu.gmall.pms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByPid(Long pid) {
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        /*
            如果父id为-1就拼查询条件，如果不是就返回全部
         */
        if(pid != -1){
            wrapper.eq("parent_id",pid);
        }
        return this.list(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoryLvl2WithSubsById(Long pid) {
        List<CategoryEntity> categoryEntities = this.categoryMapper.queryCategoryLvl2WithSubsById(pid);
        return categoryEntities;

    }

    //根据三级分类id查询一二三级分类
    @Override
    public List<CategoryEntity> queryCategoryEntitiesByCid3(Long cid3) {

        //查询3级分类
        CategoryEntity categoryEntity3 = this.categoryMapper.selectById(cid3);

        //查询2级分类
        CategoryEntity categoryEntity2 = this.categoryMapper.selectById(categoryEntity3.getParentId());

        //查询1级分类
        CategoryEntity categoryEntity1 = this.categoryMapper.selectById(categoryEntity2.getParentId());

        return Arrays.asList(categoryEntity1,categoryEntity2,categoryEntity3);


    }

}









