package com.walletledger.wallet.dto;

import com.walletledger.wallet.Wallet;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class WalletResponse {

    private final Long id;
    private final String userId;
    private final BigDecimal balance;

    public WalletResponse(Wallet wallet, BigDecimal balance) {
        this.id = wallet.getId();
        this.userId = wallet.getUserId();
        this.balance = balance;
    }
}
