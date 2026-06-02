package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.hmdp.utils.RedisConstants;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.bean.BeanUtil;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
@Slf4j
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("list")
    public Result queryTypeList() {
        //1.从redis查询判断是否存在
        String typeListJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_TYPE_LIST_KEY);
        if(StrUtil.isNotBlank(typeListJson)){
            //2.存在，直接返回
            log.info("从redis中查询店铺类型");
            List<ShopType> typeList = JSONUtil.toList(typeListJson, ShopType.class);
            return Result.ok(typeList);
        }

        //3.不存在，根据数据库查询
        log.info("从数据库中查询店铺类型");
        List<ShopType> shopTypeList = typeService
                .query().orderByAsc("sort").list();

        //4.不存在，返回错误
        if(shopTypeList == null || shopTypeList.isEmpty()){
            return Result.fail("店铺类型不存在");
        }

        //5.存在,存入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_TYPE_LIST_KEY, JSONUtil.toJsonStr(shopTypeList));

        return Result.ok(shopTypeList);
    }
}
