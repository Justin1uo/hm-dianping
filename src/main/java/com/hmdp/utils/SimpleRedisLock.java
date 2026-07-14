package com.hmdp.utils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import cn.hutool.core.lang.UUID;

public class SimpleRedisLock implements ILock {

    private StringRedisTemplate stringRedisTemplate;
    private String name;

    public SimpleRedisLock(String name ,StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static{
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
        
    }


    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程的唯一标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //尝试获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    public void unlock(){
        //调用lua脚本，实现锁的释放, 满足原子性
        stringRedisTemplate.execute(
            UNLOCK_SCRIPT,
             Collections.singletonList(KEY_PREFIX + name),
              ID_PREFIX + Thread.currentThread().getId());
    }

    /*@Override
    public void unlock() {
        //获取线程的唯一标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        
        //获取锁中的标识
        String lockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        //判断锁中的标识是否与线程的标识一致
        if (lockId.equals(threadId)) {
            //删除锁
        stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
    */


}
