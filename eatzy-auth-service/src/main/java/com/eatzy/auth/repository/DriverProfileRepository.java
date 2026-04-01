package com.eatzy.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.eatzy.auth.domain.DriverProfile;

@Repository
public interface DriverProfileRepository
        extends JpaRepository<DriverProfile, Long>, JpaSpecificationExecutor<DriverProfile> {
    Optional<DriverProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByStatus(String status);
}
