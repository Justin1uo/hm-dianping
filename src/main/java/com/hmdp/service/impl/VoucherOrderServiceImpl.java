package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.time.LocalDateTime;

import javax.annotation.Resource;

import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;
    
    @Override
    public Result seckillVoucher(Long voucherId) {
       //1.查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("优惠券不存在");
        }
       //2.判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀未开始");
        }
       //3.判断秒杀是否结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
       //4.判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        
        Long userId = UserHolder.getUser().getId();

        synchronized (userId.toString().intern()) {
            //获取当前线程的代理对象
            //为什么需要代理对象？
            //因为当前线程是调用createVoucherOrder方法的线程，而createVoucherOrder方法是@Transactional注解的，
            //所以需要代理对象来调用createVoucherOrder方法，才能实现事务的开启和提交
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }


    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //5.判断用户是否已购买
        Long userId = UserHolder.getUser().getId();

         int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
         if (count > 0) {
             return Result.fail("用户已购买过该优惠券");
         }


       //6.扣除库存
        boolean success = seckillVoucherService.update()
                                                .setSql("stock = stock - 1")
                                                //只要此线程更新时候的库存数量大于0，才更新库存（乐观锁：防止并发更新库存导致库存负数）
                                                .eq("voucher_id", voucherId).gt("stock", 0) 
                                                .update();
        if (!success) {
            return Result.fail("库存扣除失败");
        }

        

       //7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");

        voucherOrder.setId(orderId);//生成订单id，前缀为order
        voucherOrder.setUserId(UserHolder.getUser().getId());//设置用户id
        voucherOrder.setCreateTime(LocalDateTime.now());//设置创建时间
        voucherOrder.setVoucherId(voucherId);//设置优惠券id

        save(voucherOrder);
       //8.返回订单id
       return Result.ok(orderId);
    }

}
