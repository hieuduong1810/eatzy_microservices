package com.eatzy.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eatzy.payment.domain.Wallet;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    /**
     * Find user IDs from the given list that have wallet balance > 0.
     * Matches eatzy_backend: balance > 0 check in driver assignment.
     */
    @Query("SELECT w.userId FROM Wallet w WHERE w.userId IN :userIds AND w.balance > 0")
    List<Long> findUserIdsWithPositiveBalance(@Param("userIds") List<Long> userIds);
}
