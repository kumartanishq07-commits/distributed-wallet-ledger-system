package com.walletledger.transfer;

import com.walletledger.ratelimit.RateLimitExceededException;
import com.walletledger.ratelimit.RateLimiterService;
import com.walletledger.transaction.Transaction;
import com.walletledger.transfer.dto.TransferRequest;
import com.walletledger.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final IdempotencyService idempotencyService;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        // Rate limit check happens BEFORE idempotency/transfer logic - no
        // point doing any real work for a request we're about to reject.
        if (!rateLimiterService.isAllowed(request.getFromWalletId())) {
            throw new RateLimitExceededException(request.getFromWalletId());
        }

        return idempotencyService.executeIdempotent(idempotencyKey, () -> {
            Transaction transaction = transferService.transfer(request);
            return new TransferResponse(transaction);
        });
    }
}
