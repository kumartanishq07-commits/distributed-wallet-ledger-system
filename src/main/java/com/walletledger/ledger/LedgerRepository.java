package com.walletledger.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByWalletIdOrderByCreatedAtAsc(Long walletId);

    /**
     * Used by the reconciliation job: checks what ledger entries (if any)
     * exist for a given transaction, to decide whether a stuck PENDING
     * transaction actually completed, partially completed, or never
     * started.
     */
    List<LedgerEntry> findByTransactionId(Long transactionId);

    /**
     * Sum of credits minus sum of debits for a wallet = current balance.
     * This is the query that makes balance "derived" instead of "stored".
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
        FROM LedgerEntry e
        WHERE e.walletId = :walletId
        """)
    java.math.BigDecimal sumBalanceForWallet(@Param("walletId") Long walletId);
}
