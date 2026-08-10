package com.walletledger.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Call this INSIDE the same @Transactional method that writes the
     * ledger entries (see TransferService). That's what makes this the
     * "outbox pattern" rather than just "writing to two systems and
     * hoping" - the event row and the ledger rows commit or roll back
     * together, atomically, because they're one database transaction.
     */
    @SneakyThrows
    public void recordEvent(Long transactionId, String eventType, TransactionCompletedEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        outboxRepository.save(new OutboxEvent(transactionId, eventType, payload));
    }
}
