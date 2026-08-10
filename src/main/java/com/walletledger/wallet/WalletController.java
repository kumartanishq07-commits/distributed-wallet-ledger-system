package com.walletledger.wallet;

import com.walletledger.ledger.LedgerEntry;
import com.walletledger.ledger.LedgerService;
import com.walletledger.wallet.dto.CreateWalletRequest;
import com.walletledger.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final LedgerService ledgerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request);
        return new WalletResponse(wallet, walletService.getBalance(wallet.getId()));
    }

    @GetMapping("/{id}")
    public WalletResponse getWallet(@PathVariable Long id) {
        Wallet wallet = walletService.getWallet(id);
        return new WalletResponse(wallet, walletService.getBalance(id));
    }

    @GetMapping("/{id}/balance")
    public WalletResponse getBalance(@PathVariable Long id) {
        return getWallet(id);
    }

    @GetMapping("/{id}/ledger")
    public List<LedgerEntry> getLedger(@PathVariable Long id) {
        walletService.getWallet(id); // 404s if wallet doesn't exist
        return ledgerService.getEntriesForWallet(id);
    }
}
