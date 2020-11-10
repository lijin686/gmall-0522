package com.atguigu.gmall.cart.controller;


import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    //根据userId获取选中状态的购物车信息
    @GetMapping("checked/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheackedCartByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts = this.cartService.queryCheackedCartByUserId(userId);
        return ResponseVo.ok(carts);
    }


    @GetMapping("test")
    @ResponseBody
    public String test(){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        System.out.println(userInfo);
        return "hello interceptor";
    }

    @GetMapping
    public String addCart(Cart cart){
        this.cartService.addCart(cart);

        return "redirect:http://cart.gmall.com/addCart.html?skuId="+cart.getSkuId();
    }


    @GetMapping("addCart.html")
    public String queryCartBySkuId(@RequestParam("skuId")Long skuId, Model model){
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart",cart);
        return "addCart";
    }

    //查询购物车
    @GetMapping("cart.html")
    public String queryCart(Model model){
        List<Cart> carts = this.cartService.queryCart();
        model.addAttribute("carts",carts);
        return "cart";

    }

    //用户点击+ - 来更新商品数量
    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo<Cart> updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    //用户点击商品前面的选中按钮
    @PostMapping("updateStatus")
    @ResponseBody
    public ResponseVo<Cart> updateStatus(@RequestBody Cart cart){
        this.cartService.updateStatus(cart);
        return ResponseVo.ok();
    }

    //删除购物车中的商品
    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Cart> deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }
}










