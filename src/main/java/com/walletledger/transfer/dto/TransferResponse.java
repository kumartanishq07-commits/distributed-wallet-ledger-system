package com.walletledger.transfer.dto;

import com.walletledger.transaction.Transaction;
import com.walletledger.transaction.TxStatus;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TransferResponse {

    private final Long transactionId;
    private final Long fromWalletId;
    private final Long toWalletId;
    private final BigDecimal amount;
    private final TxStatus status;

    public TransferResponse(Transaction transaction) {
        this.transactionId = transaction.getId();
        this.fromWalletId = transaction.getFromWalletId();
        this.toWalletId = transaction.getToWalletId();
        this.amount = transaction.getAmount();
        this.status = transaction.getStatus();
    }
}
