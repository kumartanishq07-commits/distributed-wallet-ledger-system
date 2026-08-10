package com.walletledger.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * STEP 7: protects the transfer endpoint from being flooded by a single
 * wallet. Redis is used because a rate limiter has to be shared and fast
 * across (potentially many) instances of this app - an in-memory counter
 * like our Step 3 idempotency map would reset per-instance and defeat the
 * purpose the moment you run more than one copy of this service.
 *
 * ALGORITHM - fixed window counter (not a true token bucket):
 *   - Each wallet gets a Redis key that counts requests in the current
 *     window (e.g. "rate-limit:transfer:42").
 *   - INCR is atomic in Redis, so concurrent requests can't race each
 *     other into double-counting - each call gets a unique, correct
 *     count back.
 *   - The key is given a TTL equal to the window length, so it resets
 *     itself automatically - no cleanup job needed.
 *   - If the count exceeds the limit, the request is rejected.
 *
 * WHY NOT A "TRUE" TOKEN BUCKET: a token bucket smooths bursts more
 * gracefully (tokens refill continuously instead of resetting all at
 * once at a window boundary), but needs either a Lua script or a
 * dedicated library (e.g. Bucket4j) for atomicity. Fixed-window is
 * simpler to implement correctly and is genuinely what many real APIs
 * use (e.g. GitHub's REST API) - a reasonable, defensible choice, and
 * worth being able to explain the trade-off in an interview.
 *
 * KNOWN EDGE CASE: there's a tiny gap between the INCR and the EXPIRE
 * call below - if the app crashed in that exact gap, the key could be
 * left without a TTL. In production this would be closed with a single
 * atomic Lua script. Left as-is here since it's an extremely narrow
 * window and doesn't affect correctness in normal operation.
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(Long walletId) {
        String key = "rate-limit:transfer:" + walletId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            // First request in this window - start the TTL clock.
            redisTemplate.expire(key, WINDOW);
        }

        return count != null && count <= MAX_REQUESTS_PER_WINDOW;
    }
}
