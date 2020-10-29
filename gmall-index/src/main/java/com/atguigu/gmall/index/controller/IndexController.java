package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping({"/","index"})
    public String toIndex(Model model){
        //查询首页一级分类的id
        List<CategoryEntity> categoryEntities = this.indexService.queryLvllCategories();
        model.addAttribute("categories",categoryEntities);
        return "index";
    }
    //查询二级分类和三级分类
    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>>queryCategoryLvl2WithSubsById(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryCategoryLvl2WithSubsById(pid);
        return ResponseVo.ok(categoryEntities);
    }



    //测试锁
    @GetMapping("index/test/lock")
    @ResponseBody
    public synchronized ResponseVo<Object> testLock() throws InterruptedException {
        this.indexService.testLock();
        return ResponseVo.ok(null);
    }



}
