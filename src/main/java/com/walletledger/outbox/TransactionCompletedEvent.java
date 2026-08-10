package com.walletledger.outbox;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The actual content of a TRANSACTION_COMPLETED event, serialized to
 * JSON and stored as the OutboxEvent payload.
 */
public record TransactionCompletedEvent(
        Long transactionId,
        Long fromWalletId,
        Long toWalletId,
        BigDecimal amount,
        String status,
        Instant occurredAt
) {
}
