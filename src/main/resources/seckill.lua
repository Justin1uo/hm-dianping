--参数列表
--1. voucherId
local voucherId = ARGV[1]

--2. userId
local userId = ARGV[2]

--数据key
local stockKey = 'seckill:stock:' .. voucherId

local orderKey = 'seckill:order:' .. voucherId

--script 逻辑
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

--判断用户是否已购买 SISMEMBER
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

--扣库存，下单
redis.call('incr', stockKey, -1)
redis.call('sadd', orderKey, userId)
return 0
