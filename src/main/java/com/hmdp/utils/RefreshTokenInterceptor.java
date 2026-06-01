package com.hmdp.utils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;

import ch.qos.logback.core.joran.util.beans.BeanUtil;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;
    
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
                //1.获取token
                String token = request.getHeader("authorization");
                if (token == null) {
                    return true;
                }
                String key = RedisConstants.LOGIN_USER_KEY + token;
                //2.获取token获取redis的用户
                Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
                
                //3.判断用户是否为空
                if (userMap.isEmpty()) {
                   return true;
                }
                //4.将拿的的Hash对象转成UserDTO
                UserDTO userDTO = new UserDTO();
                cn.hutool.core.bean.BeanUtil.fillBeanWithMap(userMap, userDTO, false);
               
                //5.如果存在，保存到ThreadLocal
                UserHolder.saveUser(userDTO);
                //6.刷新token有效期
                stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
                
                //7.放行
                return true;

    }
    
    

    @Override
    public void afterCompletion( HttpServletRequest request, HttpServletResponse response, Object handler,
         @Nullable Exception ex) throws Exception {
                //1.从ThreadLocal中移除用户
                UserHolder.removeUser();
                
            }

}
