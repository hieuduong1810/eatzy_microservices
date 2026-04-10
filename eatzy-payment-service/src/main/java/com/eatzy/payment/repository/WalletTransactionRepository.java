package com.eatzy.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.payment.domain.WalletTransaction;
import com.eatzy.payment.domain.enums.TransactionType;

import java.util.List;

@Repository
public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, Long>, JpaSpecificationExecutor<WalletTransaction> {
    List<WalletTransaction> findByWalletId(Long walletId);

    List<WalletTransaction> findByOrderId(Long orderId);

    List<WalletTransaction> findByWalletIdAndTransactionType(Long walletId, TransactionType transactionType);

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
