local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local max_limit = tonumber(ARGV[3])
local request_id = ARGV[4]

local clear_before = now - (window * 1000)

-- Clear old records outside window
redis.call('ZREMRANGEBYSCORE', key, '-inf', clear_before)

-- Count requests in current window
local current_requests = redis.call('ZCARD', key)

if current_requests < max_limit then
    redis.call('ZADD', key, now, request_id)
    redis.call('EXPIRE', key, window + 1)
    return 1 -- Allowed
else
    return 0 -- Denied
end