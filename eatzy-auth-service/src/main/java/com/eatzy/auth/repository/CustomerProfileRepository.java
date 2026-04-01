package com.eatzy.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.auth.domain.CustomerProfile;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfile, Long>, JpaSpecificationExecutor<CustomerProfile> {
    Optional<CustomerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
