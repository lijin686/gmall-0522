package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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



}
