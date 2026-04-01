package com.eatzy.systemconfig.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.systemconfig.domain.SystemConfiguration;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.systemconfig.repository.SystemConfigurationRepository;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class SystemConfigurationService {
    private final SystemConfigurationRepository systemConfigurationRepository;

    public SystemConfigurationService(SystemConfigurationRepository systemConfigurationRepository) {
        this.systemConfigurationRepository = systemConfigurationRepository;
    }

    public boolean existsByConfigKey(String configKey) {
        return systemConfigurationRepository.existsByConfigKey(configKey);
    }

    public SystemConfiguration getSystemConfigurationById(Long id) {
        Optional<SystemConfiguration> configOpt = this.systemConfigurationRepository.findById(id);
        return configOpt.orElse(null);
    }

    public SystemConfiguration getSystemConfigurationByKey(String configKey) {
        Optional<SystemConfiguration> configOpt = this.systemConfigurationRepository.findByConfigKey(configKey);
        return configOpt.orElse(null);
    }

    public SystemConfiguration createSystemConfiguration(SystemConfiguration systemConfiguration)
            throws IdInvalidException {
        // check duplicate key
        if (this.existsByConfigKey(systemConfiguration.getConfigKey())) {
            throw new IdInvalidException("Configuration key already exists: " + systemConfiguration.getConfigKey());
        }

        systemConfiguration.setUpdatedAt(Instant.now());
        return systemConfigurationRepository.save(systemConfiguration);
    }

    public SystemConfiguration updateSystemConfiguration(SystemConfiguration systemConfiguration)
            throws IdInvalidException {
        // check id
        SystemConfiguration currentConfig = getSystemConfigurationById(systemConfiguration.getId());
        if (currentConfig == null) {
            throw new IdInvalidException("System configuration not found with id: " + systemConfiguration.getId());
        }

        // update fields
        if (systemConfiguration.getConfigKey() != null) {
            // check duplicate key if key is changed
            if (!currentConfig.getConfigKey().equals(systemConfiguration.getConfigKey())) {
                if (this.existsByConfigKey(systemConfiguration.getConfigKey())) {
                    throw new IdInvalidException(
                            "Configuration key already exists: " + systemConfiguration.getConfigKey());
                }
            }
            currentConfig.setConfigKey(systemConfiguration.getConfigKey());
        }
        if (systemConfiguration.getConfigValue() != null) {
            currentConfig.setConfigValue(systemConfiguration.getConfigValue());
        }
        if (systemConfiguration.getDescription() != null) {
            currentConfig.setDescription(systemConfiguration.getDescription());
        }

        currentConfig.setUpdatedAt(Instant.now());
        return systemConfigurationRepository.save(currentConfig);
    }

    public ResultPaginationDTO getAllSystemConfigurations(Specification<SystemConfiguration> spec, Pageable pageable) {
        Page<SystemConfiguration> page = this.systemConfigurationRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent());
        return result;
    }

    public void deleteSystemConfiguration(Long id) {
        this.systemConfigurationRepository.deleteById(id);
    }
}
