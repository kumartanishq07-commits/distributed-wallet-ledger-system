package com.walletledger.ledger;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An immutable record of one side of a money movement.
 * Every transfer creates exactly two rows: one DEBIT (money leaving a
 * wallet) and one CREDIT (money entering a wallet). We never update a
 * wallet's balance directly - balance is always the sum of its entries.
 *
 * transactionId is nullable because wallet-opening entries (initial
 * funding at wallet creation) aren't tied to a transfer transaction.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long transactionId;

    @Column(nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType type;

    @Column(nullable = false)
    private BigDecimal amount;

    /** Running balance of the wallet immediately after this entry was applied. */
    @Column(nullable = false)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public LedgerEntry(Long transactionId, Long walletId, EntryType type, BigDecimal amount, BigDecimal balanceAfter) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }
}
