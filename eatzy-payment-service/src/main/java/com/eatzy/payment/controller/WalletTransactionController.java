package com.eatzy.payment.controller;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.domain.WalletTransaction;
import com.eatzy.payment.dto.response.ResWalletTransactionDTO;
import com.eatzy.payment.service.WalletTransactionService;
import com.turkraft.springfilter.boot.Filter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/wallet-transactions")
public class WalletTransactionController {
    
    private final WalletTransactionService walletTransactionService;

    public WalletTransactionController(WalletTransactionService walletTransactionService) {
        this.walletTransactionService = walletTransactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<ResWalletTransactionDTO> deposit(
            @RequestParam("walletId") Long walletId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "description", required = false) String description) throws IdInvalidException {
        return ResponseEntity.ok(walletTransactionService.depositToWallet(walletId, amount, description));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ResWalletTransactionDTO> withdraw(
            @RequestParam("walletId") Long walletId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "description", required = false) String description) throws IdInvalidException {
        return ResponseEntity.ok(walletTransactionService.withdrawFromWallet(walletId, amount, description));
    }

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllTransactions(
            @Filter Specification<WalletTransaction> spec, Pageable pageable) {
        // Here we could enforce security to only allow the user to see their own transactions
        return ResponseEntity.ok(walletTransactionService.getWalletTransactionsByWalletIdWithSpec(null, spec, pageable));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ResultPaginationDTO> getTransactionsByWallet(
            @PathVariable Long walletId,
            @Filter Specification<WalletTransaction> spec, Pageable pageable) {
        return ResponseEntity.ok(walletTransactionService.getWalletTransactionsByWalletIdWithSpec(walletId, spec, pageable));
    }
}
