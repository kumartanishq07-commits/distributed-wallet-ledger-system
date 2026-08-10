package com.walletledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // needed for OutboxPoller and, later, the reconciliation job
public class WalletLedgerEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletLedgerEngineApplication.class, args);
    }

}
