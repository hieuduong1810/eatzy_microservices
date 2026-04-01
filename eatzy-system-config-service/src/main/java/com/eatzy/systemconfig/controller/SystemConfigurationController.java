package com.eatzy.systemconfig.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import com.eatzy.systemconfig.domain.SystemConfiguration;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.systemconfig.service.SystemConfigurationService;
import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.exception.IdInvalidException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1")
public class SystemConfigurationController {
    private final SystemConfigurationService systemConfigurationService;

    public SystemConfigurationController(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    @PostMapping("/system-configurations")
    @ApiMessage("Create system configuration")
    public ResponseEntity<SystemConfiguration> createSystemConfiguration(@RequestBody SystemConfiguration systemConfiguration) throws IdInvalidException {
        SystemConfiguration createdConfig = systemConfigurationService.createSystemConfiguration(systemConfiguration);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdConfig);
    }

    @PutMapping("/system-configurations")
    @ApiMessage("Update system configuration")
    public ResponseEntity<SystemConfiguration> updateSystemConfiguration(@RequestBody SystemConfiguration systemConfiguration) throws IdInvalidException {
        SystemConfiguration updatedConfig = systemConfigurationService.updateSystemConfiguration(systemConfiguration);
        return ResponseEntity.ok(updatedConfig);
    }

    @GetMapping("/system-configurations")
    @ApiMessage("Get all system configurations")
    public ResponseEntity<ResultPaginationDTO> getAllSystemConfigurations(
            @Filter Specification<SystemConfiguration> spec, Pageable pageable) {
        ResultPaginationDTO result = systemConfigurationService.getAllSystemConfigurations(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/system-configurations/{id}")
    @ApiMessage("Get system configuration by id")
    public ResponseEntity<SystemConfiguration> getSystemConfigurationById(@PathVariable("id") Long id) throws IdInvalidException {
        SystemConfiguration config = systemConfigurationService.getSystemConfigurationById(id);
        if (config == null) {
            throw new IdInvalidException("System configuration not found with id: " + id);
        }
        return ResponseEntity.ok(config);
    }

    @GetMapping("/system-configurations/key/{configKey}")
    @ApiMessage("Get system configuration by key")
    public ResponseEntity<SystemConfiguration> getSystemConfigurationByKey(@PathVariable("configKey") String configKey) throws IdInvalidException {
        SystemConfiguration config = systemConfigurationService.getSystemConfigurationByKey(configKey);
        if (config == null) {
            throw new IdInvalidException("System configuration not found with key: " + configKey);
        }
        return ResponseEntity.ok(config);
    }

    @DeleteMapping("/system-configurations/{id}")
    @ApiMessage("Delete system configuration by id")
    public ResponseEntity<Void> deleteSystemConfiguration(@PathVariable("id") Long id) throws IdInvalidException {
        SystemConfiguration config = systemConfigurationService.getSystemConfigurationById(id);
        if (config == null) {
            throw new IdInvalidException("System configuration not found with id: " + id);
        }
        systemConfigurationService.deleteSystemConfiguration(id);
        return ResponseEntity.ok().body(null);
    }
}
