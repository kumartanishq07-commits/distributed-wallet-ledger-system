package com.walletledger.wallet;

import com.walletledger.common.WalletNotFoundException;
import com.walletledger.ledger.EntryType;
import com.walletledger.ledger.LedgerService;
import com.walletledger.wallet.dto.CreateWalletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;

    @Transactional
    public Wallet createWallet(CreateWalletRequest request) {
        Wallet wallet = walletRepository.save(new Wallet(request.getUserId()));

        // Opening balance becomes a CREDIT ledger entry, not a stored field.
        // NOTE: a strict double-entry system would offset this against a
        // "bank/system" account so every CREDIT has a matching DEBIT
        // somewhere. We're simplifying that for now since there's no real
        // money source yet - this is called out here on purpose so it's
        // easy to explain as a known simplification, not an oversight.
        if (request.getOpeningBalance().compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.recordEntry(null, wallet.getId(), EntryType.CREDIT, request.getOpeningBalance());
        }

        return wallet;
    }

    public Wallet getWallet(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    public BigDecimal getBalance(Long walletId) {
        getWallet(walletId); // throws WalletNotFoundException if it doesn't exist
        return ledgerService.getBalance(walletId);
    }
}
