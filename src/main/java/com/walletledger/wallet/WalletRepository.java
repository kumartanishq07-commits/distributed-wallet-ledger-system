package com.walletledger.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(String userId);

    /**
     * STEP 4 fix: locks the wallet row for the duration of the current
     * transaction (translates to SELECT ... FOR UPDATE in MySQL). Any
     * other transaction trying to read this same row with this method
     * will BLOCK until the current transaction commits or rolls back -
     * that's what serializes concurrent transfers from the same wallet
     * and closes the race condition the test proved.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);
}
