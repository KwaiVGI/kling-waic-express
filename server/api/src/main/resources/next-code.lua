local key = KEYS[1]
local startValue = tonumber(ARGV[1])

local current = redis.call('GET', key)
if not current then
    redis.call('SET', key, startValue)
end

local newValue = redis.call('INCR', key)
return newValue
