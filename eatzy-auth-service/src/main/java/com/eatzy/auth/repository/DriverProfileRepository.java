package com.eatzy.auth.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.eatzy.auth.domain.DriverProfile;

@Repository
public interface DriverProfileRepository
        extends JpaRepository<DriverProfile, Long>, JpaSpecificationExecutor<DriverProfile> {
    Optional<DriverProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByStatus(String status);

    @Query("SELECT dp FROM DriverProfile dp WHERE dp.user.id IN :userIds AND dp.status = 'AVAILABLE'")
    List<DriverProfile> findByUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT dp FROM DriverProfile dp WHERE dp.user.id IN :userIds AND dp.status = 'AVAILABLE' AND dp.codLimit >= :minCodLimit")
    List<DriverProfile> findByUserIdsWithCodLimit(@Param("userIds") List<Long> userIds,
            @Param("minCodLimit") BigDecimal minCodLimit);
}
