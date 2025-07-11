local key = KEYS[1]
local step = tonumber(ARGV[1])
local startValue = tonumber(ARGV[2])

local current = redis.call('GET', key)
if not current then
    redis.call('SET', key, startValue)
end

local newValue = redis.call('INCRBY', key, step)
return newValue
