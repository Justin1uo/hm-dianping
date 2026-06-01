package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;



    /**
     * 发送手机验证码
     * @param phone 手机号
     * @param session 会话对象，用于保存验证码
     * @return 发送结果
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
             //2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3.符合，则生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到redis    类似于set key value EX 120
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码
        log.debug("发送验证码：{}", code); 
        //返回ok
        return Result.ok();
    }
    
    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @param session 会话对象，用于保存登录信息
     * @return 登录结果
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
         //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
             //2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3.符合，则校验验证码  TODO 从redis中获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if(cacheCode == null || !loginForm.getCode().equals(cacheCode)){
             return Result.fail("验证码错误");
        }

        User user = query().eq("phone", phone).one();
        if(user == null){
            //4.如果不存在，创建该用户
            user = createUserWithPhone(phone);
        }
        
        //保存用户信息到redis

        //1.随机生成一个token
        String token = cn.hutool.core.lang.UUID.randomUUID().toString(true);
        //2.将User对象转成Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                                                    CopyOptions.create()
                                                        .setIgnoreNullValue(true)
                                                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
      
        //3.存储
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, userMap);
        //4.设置过期时间
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        
        //返回token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
    
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));
        //保存用户到数据库
        save(user);
        return user;
    }

}
