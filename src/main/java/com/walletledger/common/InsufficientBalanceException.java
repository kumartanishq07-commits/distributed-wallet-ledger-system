package com.walletledger.common;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Long walletId) {
        super("Wallet " + walletId + " has insufficient balance for this transfer");
    }
}
