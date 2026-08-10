package com.walletledger.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * STEP 5: an event waiting to be published to Kafka. This row is always
 * written inside the SAME database transaction as the ledger entries it
 * describes (see TransferService) - so either both the transaction AND
 * its event are saved, or neither are. A separate poller (OutboxPoller)
 * publishes these to Kafka afterward and marks them published.
 *
 * This is what guarantees we never lose an event even if Kafka happens
 * to be down at the moment the transfer completes - the event just sits
 * here as "not yet published" until Kafka is reachable again.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean published = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public OutboxEvent(Long transactionId, String eventType, String payload) {
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.payload = payload;
    }
}
