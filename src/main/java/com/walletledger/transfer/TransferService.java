package com.walletledger.transfer;

import com.walletledger.common.InsufficientBalanceException;
import com.walletledger.common.WalletNotFoundException;
import com.walletledger.ledger.EntryType;
import com.walletledger.ledger.LedgerService;
import com.walletledger.outbox.OutboxService;
import com.walletledger.outbox.TransactionCompletedEvent;
import com.walletledger.transaction.Transaction;
import com.walletledger.transaction.TransactionRepository;
import com.walletledger.transaction.TxStatus;
import com.walletledger.transfer.dto.TransferRequest;
import com.walletledger.wallet.Wallet;
import com.walletledger.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * STEP 5 addition: after the transfer succeeds, we now also write an
 * OutboxEvent - in the SAME @Transactional method, so it commits
 * atomically with the ledger entries. A separate poller (OutboxPoller)
 * publishes it to Kafka afterward. We are NOT calling Kafka directly
 * from here - that's the whole point of the outbox pattern.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;

    @Transactional
    public Transaction transfer(TransferRequest request) {
        Wallet from = walletRepository.findByIdForUpdate(request.getFromWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getFromWalletId()));
        Wallet to = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getToWalletId()));

        BigDecimal currentFromBalance = ledgerService.getBalance(from.getId());
        if (currentFromBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(from.getId());
        }

        Transaction transaction = new Transaction(
                from.getId(), to.getId(), request.getAmount(), TxStatus.PENDING
        );
        transaction = transactionRepository.save(transaction);

        ledgerService.recordEntry(transaction.getId(), from.getId(), EntryType.DEBIT, request.getAmount());
        ledgerService.recordEntry(transaction.getId(), to.getId(), EntryType.CREDIT, request.getAmount());

        transaction.setStatus(TxStatus.SUCCESS);
        transaction = transactionRepository.save(transaction);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transaction.getId(),
                transaction.getFromWalletId(),
                transaction.getToWalletId(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                Instant.now()
        );
        outboxService.recordEvent(transaction.getId(), "TRANSACTION_COMPLETED", event);

        return transaction;
    }
}
