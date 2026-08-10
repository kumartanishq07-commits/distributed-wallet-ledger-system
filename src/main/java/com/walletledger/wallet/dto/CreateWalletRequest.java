package com.walletledger.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateWalletRequest {

    @NotBlank
    private String userId;

    @NotNull
    @PositiveOrZero
    private BigDecimal openingBalance;
}
