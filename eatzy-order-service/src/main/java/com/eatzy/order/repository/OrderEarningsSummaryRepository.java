package com.eatzy.order.repository;

import com.eatzy.order.domain.OrderEarningsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderEarningsSummaryRepository extends JpaRepository<OrderEarningsSummary, Long>, JpaSpecificationExecutor<OrderEarningsSummary> {
    Optional<OrderEarningsSummary> findByOrderId(Long orderId);
    List<OrderEarningsSummary> findByDriverId(Long driverId);
    List<OrderEarningsSummary> findByRestaurantId(Long restaurantId);
    List<OrderEarningsSummary> findByRecordedAtBetween(Instant startDate, Instant endDate);

    @Query("SELECT SUM(e.driverNetEarning) FROM OrderEarningsSummary e WHERE e.driverId = :driverId")
    BigDecimal sumDriverEarnings(@Param("driverId") Long driverId);

    @Query("SELECT SUM(e.restaurantNetEarning) FROM OrderEarningsSummary e WHERE e.restaurantId = :restaurantId")
    BigDecimal sumRestaurantEarnings(@Param("restaurantId") Long restaurantId);

    @Query("SELECT SUM(e.platformTotalEarning) FROM OrderEarningsSummary e WHERE e.recordedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumPlatformEarnings(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
