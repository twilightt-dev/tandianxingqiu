-- ARGV[1]：用户 ID
local userId = ARGV[1]

local stockKey = KEYS[1]
local userKey = KEYS[2]

-- 库存未初始化或不足
local stock = tonumber(redis.call('get', stockKey))
if not stock or stock <= 0 then
	return 1
end

-- 重复购买
if redis.call('sismember', userKey, userId) == 1 then
	return 2
end

redis.call('incrby', stockKey, -1)
redis.call('sadd', userKey, userId)

return 0