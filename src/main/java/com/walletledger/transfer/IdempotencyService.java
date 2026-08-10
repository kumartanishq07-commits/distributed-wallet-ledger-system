package com.walletledger.transfer;

import com.walletledger.transfer.dto.TransferResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * STEP 3: prevents a retried request from executing the same transfer twice.
 *
 * Storage note: this is an in-memory ConcurrentHashMap for now - simple and
 * enough to prove the concept, but it resets if the app restarts and won't
 * work across multiple app instances. Step 7 replaces this with Redis,
 * which solves both problems (persists, and is shared across instances).
 *
 * Concurrency note: ConcurrentHashMap.computeIfAbsent() guarantees the
 * mapping function runs at most once per key even if two threads call it
 * at the exact same time for the same key - so this already gives us real
 * duplicate-suppression under concurrent retries, not just single-threaded
 * correctness.
 */
@Service
public class IdempotencyService {

    private final Map<String, TransferResponse> processedRequests = new ConcurrentHashMap<>();

    public TransferResponse executeIdempotent(String idempotencyKey, Supplier<TransferResponse> action) {
        return processedRequests.computeIfAbsent(idempotencyKey, key -> action.get());
    }
}
