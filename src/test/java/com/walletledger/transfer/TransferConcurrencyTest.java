package com.walletledger.transfer;

import com.walletledger.ledger.LedgerService;
import com.walletledger.transfer.dto.TransferRequest;
import com.walletledger.wallet.Wallet;
import com.walletledger.wallet.WalletService;
import com.walletledger.wallet.dto.CreateWalletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STEP 4: proves a race condition where concurrent transfers from the
 * same wallet can bypass the "sufficient balance" check and overdraw it.
 *
 * The scenario: a wallet has 500. We fire 10 concurrent transfer attempts
 * of 100 each. Only 5 SHOULD ever succeed - but without locking, multiple
 * threads can all read "current balance = 500" before any of them has
 * committed their debit, so all 10 (or more than 5, at least) pass the
 * balance check and the wallet ends up negative.
 *
 * This is an integration test - it needs the real local MySQL running
 * (the same one the app itself uses), since we're testing real database
 * transaction/locking behavior, not something a mock can simulate.
 *
 * Run it now (before locking is added) and expect it to FAIL. That
 * failure is not a bug in the test - it's proof the bug we're about to
 * fix actually exists.
 */
@SpringBootTest
class TransferConcurrencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void concurrentTransfersCannotOverdrawWallet() throws InterruptedException {
        // Unique userId suffix each run, so re-running the test doesn't
        // collide with the previous run's wallets (userId is unique).
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Wallet from = walletService.createWallet(
                newRequest("concurrency-from-" + suffix, new BigDecimal("500")));
        Wallet to = walletService.createWallet(
                newRequest("concurrency-to-" + suffix, BigDecimal.ZERO));

        int threadCount = 10;
        BigDecimal amountPerTransfer = new BigDecimal("100");
        // 500 balance / 100 per transfer = only 5 of these 10 attempts
        // should ever be allowed to succeed.

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    TransferRequest request = new TransferRequest();
                    request.setFromWalletId(from.getId());
                    request.setToWalletId(to.getId());
                    request.setAmount(amountPerTransfer);
                    try {
                        transferService.transfer(request);
                    } catch (Exception ignored) {
                        // Expected for attempts correctly rejected once
                        // the wallet is out of balance.
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalBalance = ledgerService.getBalance(from.getId());
        System.out.println("Final balance after 10 concurrent transfer attempts (expected: 0, never negative): " + finalBalance);

        assertTrue(finalBalance.compareTo(BigDecimal.ZERO) >= 0,
                "Wallet went NEGATIVE! Final balance: " + finalBalance
                        + " -- this proves the race condition: multiple threads read the same "
                        + "balance before any of them committed, so more than 5 transfers were "
                        + "allowed to succeed.");
    }

    private CreateWalletRequest newRequest(String userId, BigDecimal openingBalance) {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);
        request.setOpeningBalance(openingBalance);
        return request;
    }
}
