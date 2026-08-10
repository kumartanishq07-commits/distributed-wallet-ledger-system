package com.walletledger.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Used by the reconciliation job: finds transactions that have been
     * sitting in PENDING for longer than the given cutoff time.
     */
    List<Transaction> findByStatusAndCreatedAtBefore(TxStatus status, Instant cutoff);
}
