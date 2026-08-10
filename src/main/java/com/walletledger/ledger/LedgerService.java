package com.walletledger.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    public BigDecimal getBalance(Long walletId) {
        return ledgerRepository.sumBalanceForWallet(walletId);
    }

    public List<LedgerEntry> getEntriesForWallet(Long walletId) {
        return ledgerRepository.findByWalletIdOrderByCreatedAtAsc(walletId);
    }

    /**
     * Records one ledger entry and returns it. Always call this inside the
     * same @Transactional boundary as the rest of the transfer/creation
     * logic, so the entry can never be written without the operation that
     * caused it (and vice versa).
     */
    public LedgerEntry recordEntry(Long transactionId, Long walletId, EntryType type, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(walletId);
        BigDecimal newBalance = type == EntryType.CREDIT
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

        LedgerEntry entry = new LedgerEntry(transactionId, walletId, type, amount, newBalance);
        return ledgerRepository.save(entry);
    }
}
