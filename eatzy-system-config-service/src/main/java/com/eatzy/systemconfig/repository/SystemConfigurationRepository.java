package com.eatzy.systemconfig.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.systemconfig.domain.SystemConfiguration;

import java.util.Optional;

@Repository
public interface SystemConfigurationRepository
        extends JpaRepository<SystemConfiguration, Long>, JpaSpecificationExecutor<SystemConfiguration> {
    Optional<SystemConfiguration> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);
}
