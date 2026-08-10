package com.walletledger.common;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(Long walletId) {
        super("Wallet not found: " + walletId);
    }
}
