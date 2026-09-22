package com.gameexpert.chat.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRateLimitService {

    private static final DefaultRedisScript<Long> LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local count = tonumber(redis.call('GET', KEYS[1]) or '0');
            if count >= 5 then
                return 0;
            end
            
            local updated = redis.call('INCR', KEYS[1])
            if updated == 1 then
                redis.call('EXPIRE', KEYS[1], 10)
            end
            
            return 1;
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean allow(Long playerId) {
        // TODO Lv 19: 횟수 확인부터 최초 만료 설정까지 원자적으로 실행합니다.
        String key = "chat:limit:" + playerId;
        Long result = redisTemplate.execute(LIMIT_SCRIPT, List.of(key));

        // String value = redisTemplate.opsForValue().get(key);
        // int count = value == null ? 0 : Integer.parseInt(value);
        // if (count >= 5) {
        //     return false;
        // }

        // Long updated = redisTemplate.opsForValue().increment(key);
        // if (updated == 1L) {
        //     redisTemplate.expire(key, Duration.ofSeconds(10));
        // }
        return Long.valueOf(1L).equals(result);
    }
}
