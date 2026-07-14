package com.hmdp.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param lockKey 锁的key
     * @return 是否获取成功
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     * @param lockKey 锁的key
     */
    void unlock();
}
