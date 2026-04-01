package com.eatzy.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eatzy.payment.domain.Voucher;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {
    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT v FROM Voucher v JOIN v.restaurantIds r WHERE r = :restaurantId")
    List<Voucher> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT DISTINCT v FROM Voucher v LEFT JOIN v.restaurantIds r WHERE " +
            "(r = :restaurantId OR v.restaurantIds IS EMPTY) AND " +
            "v.startDate <= :currentTime AND v.endDate >= :currentTime AND " +
            "v.remainingQuantity > 0")
    List<Voucher> findAvailableVouchersForOrder(
            @Param("restaurantId") Long restaurantId,
            @Param("currentTime") Instant currentTime);
}
