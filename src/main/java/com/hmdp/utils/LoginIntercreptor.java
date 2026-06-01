package com.hmdp.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.web.servlet.HandlerInterceptor;



public class LoginIntercreptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
               //1.判断是否需要拦截(依据就是ThreadLocal中是否有用户信息)
            if (UserHolder.getUser() != null) {
                response.setStatus(401);
                   return false;
            }

            return true;

    }
}
