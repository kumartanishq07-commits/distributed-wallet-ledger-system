package com.walletledger.ratelimit;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(Long walletId) {
        super("Rate limit exceeded for wallet " + walletId + ". Try again in a minute.");
    }
}
