package com.hmdp.config;

import javax.annotation.Resource;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.hmdp.utils.LoginIntercreptor;
import com.hmdp.utils.RefreshTokenInterceptor;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new LoginIntercreptor())
        .excludePathPatterns(
            "/shop/**",
            "/shop-type/**",
            "/voucher/**",
            "/blog/hot",
            "/user/code",
            "/user/login"
        ).order(1);

        // 刷新token拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);
       
    }

}
