package com.gameexpert.chat.service;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRateLimitService {
    private static final DefaultRedisScript<Long> CHATLIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current >= 5 then
                return 0
            else
                redis.call('INCR', KEYS[1])
                if current == 0 then
                    redis.call('EXPIRE', KEYS[1], 10)
                end
                return 1
            end
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean allow(Long playerId) {
        String key = "chat:limit:" + playerId;
        Long result = redisTemplate.execute(CHATLIMIT_SCRIPT, List.of(key));
        return result == 1L;
    }
}
