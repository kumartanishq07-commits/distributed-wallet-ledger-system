package com.walletledger.reconciliation;

import com.walletledger.ledger.EntryType;
import com.walletledger.ledger.LedgerEntry;
import com.walletledger.ledger.LedgerRepository;
import com.walletledger.ledger.LedgerService;
import com.walletledger.transaction.Transaction;
import com.walletledger.transaction.TransactionRepository;
import com.walletledger.transaction.TxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * STEP 6: finds transactions that have been sitting in PENDING for too
 * long and resolves them, instead of leaving them stuck forever.
 *
 * IMPORTANT DESIGN NOTE, worth understanding (and worth explaining in an
 * interview): with the CURRENT TransferService, a transaction can never
 * actually get stuck in PENDING in this database, because the PENDING
 * insert and every ledger write happen inside ONE @Transactional method -
 * if the app crashes partway through, the whole thing rolls back
 * together, including the PENDING row itself. So right now this job will
 * almost never find anything to do.
 *
 * WHY WE STILL BUILD IT: real payment systems often can't keep everything
 * in one local transaction - e.g. if a step calls an external payment
 * gateway, or is split across multiple services/messages, a transaction
 * absolutely CAN get stuck in an in-between state, and something has to
 * notice and resolve it. This job is written to handle that general
 * case correctly, and you can prove it works by manually inserting a
 * stuck PENDING row (see README) to simulate that scenario.
 *
 * Resolution logic per stuck transaction, based on what's actually in
 * the ledger for it:
 *   - Both a DEBIT and a CREDIT entry exist  -> the transfer actually
 *     completed, just the status update didn't. Mark it SUCCESS.
 *   - No entries exist at all                -> nothing happened. Safe
 *     to mark FAILED.
 *   - Only ONE entry exists (a partial/interrupted write) -> write the
 *     offsetting entry to cancel it out, then mark REVERSED. This keeps
 *     the ledger balanced even in this edge case.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    // How long a transaction can sit in PENDING before we consider it
    // "stuck". Short on purpose here so it's easy to test - a real system
    // would likely use several minutes.
    private static final long STUCK_THRESHOLD_SECONDS = 30;

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final LedgerService ledgerService;

    @Scheduled(fixedDelay = 15000)
    public void reconcileStuckTransactions() {
        Instant cutoff = Instant.now().minus(STUCK_THRESHOLD_SECONDS, ChronoUnit.SECONDS);
        List<Transaction> stuck = transactionRepository.findByStatusAndCreatedAtBefore(TxStatus.PENDING, cutoff);

        if (stuck.isEmpty()) {
            return; // nothing to do - most of the time, this is the case
        }

        log.info("Reconciliation found {} stuck PENDING transaction(s)", stuck.size());
        for (Transaction transaction : stuck) {
            resolve(transaction);
        }
    }

    @Transactional
    void resolve(Transaction transaction) {
        List<LedgerEntry> entries = ledgerRepository.findByTransactionId(transaction.getId());

        if (entries.size() == 2) {
            transaction.setStatus(TxStatus.SUCCESS);
            transactionRepository.save(transaction);
            log.info("Reconciled transaction {} -> SUCCESS (both ledger entries were already present)", transaction.getId());

        } else if (entries.isEmpty()) {
            transaction.setStatus(TxStatus.FAILED);
            transactionRepository.save(transaction);
            log.info("Reconciled transaction {} -> FAILED (no ledger entries were ever written, nothing to undo)", transaction.getId());

        } else {
            // Exactly one entry exists - write the offsetting entry to
            // cancel it out, then mark the transaction reversed.
            LedgerEntry existing = entries.get(0);
            EntryType offsettingType = existing.getType() == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT;
            ledgerService.recordEntry(transaction.getId(), existing.getWalletId(), offsettingType, existing.getAmount());

            transaction.setStatus(TxStatus.REVERSED);
            transactionRepository.save(transaction);
            log.info("Reconciled transaction {} -> REVERSED (only one ledger entry existed, wrote an offsetting entry to cancel it)", transaction.getId());
        }
    }
}
