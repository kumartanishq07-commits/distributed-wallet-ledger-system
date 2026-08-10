package com.walletledger.wallet;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * STEP 2 change: the "balance" column is gone. A wallet no longer stores
 * its own balance - it is always derived by summing LedgerEntry rows
 * (see LedgerService.getBalance()). This is the core design decision of
 * the whole project: balance can never silently drift from the ledger,
 * because there is nothing to drift - the ledger IS the balance.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    public Wallet(String userId) {
        this.userId = userId;
    }
}
