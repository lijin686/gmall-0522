package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.bouncycastle.cms.PasswordRecipientId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {


    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties jwtProperties;


    public void login(String loginName, String password,  HttpServletRequest request,HttpServletResponse response) {

        // 1 远程调用ums接口查询用户
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUser(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();


        // 2 对用户信息判空
        if(userEntity == null){
            throw new UserException("用户名或密码错误");
        }

        // 3 组装载荷信息
        Map<String,Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("userName",userEntity.getUsername());

        // 4 防止盗用，加入当前用户的ip
        String ip = IpUtil.getIpAddressAtService(request);
        map.put("ip",ip);


        try {
            // 5 生成token
            String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExpire());


            // 6 把token放入cookie中
            CookieUtils.setCookie(request,response,this.jwtProperties.getCookieName(),token,this.jwtProperties.getExpire()*60);
            // 7 登录成功之后显示昵称
            CookieUtils.setCookie(request,response,this.jwtProperties.getNickName(),userEntity.getNickname(),this.jwtProperties.getExpire()*60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



















