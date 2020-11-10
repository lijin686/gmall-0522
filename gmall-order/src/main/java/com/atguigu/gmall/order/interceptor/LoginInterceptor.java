package com.atguigu.gmall.order.interceptor;


import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    public static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String userId = request.getHeader("userId");
        if(userId == null){
            //重定向到登录页面 TODO;
            return false;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(Long.valueOf(userId));
        THREAD_LOCAL.set(userInfo);
        return true;

    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        THREAD_LOCAL.remove();

    }
}
