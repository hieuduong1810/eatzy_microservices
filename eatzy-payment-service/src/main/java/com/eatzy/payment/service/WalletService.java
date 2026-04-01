package com.eatzy.payment.service;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.domain.Wallet;
import com.eatzy.payment.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id).orElse(null);
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Wallet createWallet(Long userId) throws IdInvalidException {
        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new IdInvalidException("Wallet already exists for user ID: " + userId);
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();
        return walletRepository.save(wallet);
    }

    @Transactional
    public void addBalance(Long walletId, BigDecimal amount) throws IdInvalidException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IdInvalidException("Amount to add must be greater than 0");
        }

        Wallet wallet = getWalletById(walletId);
        if (wallet == null) {
            throw new IdInvalidException("Wallet not found with id: " + walletId);
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void subtractBalance(Long walletId, BigDecimal amount) throws IdInvalidException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IdInvalidException("Amount to subtract must be greater than 0");
        }

        Wallet wallet = getWalletById(walletId);
        if (wallet == null) {
            throw new IdInvalidException("Wallet not found with id: " + walletId);
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IdInvalidException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }
}
