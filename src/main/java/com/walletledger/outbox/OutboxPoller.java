package com.walletledger.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STEP 5: runs every 5 seconds, finds outbox rows not yet published,
 * sends them to Kafka, and marks them published ONLY after a successful
 * send. If Kafka is down, the send throws, the row stays unpublished,
 * and we just try again on the next tick - no event is ever lost, and
 * none are marked "sent" unless they actually were.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private static final String TOPIC = "wallet-transactions";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event.getPayload()).get();
                event.setPublished(true);
                outboxRepository.save(event);
                log.info("Published outbox event id={} transactionId={} to Kafka", event.getId(), event.getTransactionId());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event id={} - will retry on next poll. Error: {}",
                        event.getId(), e.getMessage());
                // Deliberately NOT marking as published - it stays in the
                // "pending" query result and gets retried next tick.
            }
        }
    }
}
