package com.eatzy.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import com.eatzy.auth.domain.CustomerProfile;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.domain.res.customerProfile.ResCustomerProfileDTO;
import com.eatzy.auth.service.CustomerProfileService;
import com.eatzy.auth.mapper.CustomerProfileMapper;
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
public class CustomerProfileController {
    private final CustomerProfileService customerProfileService;
    private final CustomerProfileMapper customerProfileMapper;

    public CustomerProfileController(CustomerProfileService customerProfileService,
            CustomerProfileMapper customerProfileMapper) {
        this.customerProfileService = customerProfileService;
        this.customerProfileMapper = customerProfileMapper;
    }

    @PostMapping("/customer-profiles")
    @ApiMessage("Create customer profile")
    public ResponseEntity<ResCustomerProfileDTO> createCustomerProfile(@RequestBody CustomerProfile customerProfile)
            throws IdInvalidException {
        CustomerProfile createdProfile = customerProfileService.createCustomerProfile(customerProfile);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerProfileMapper.convertToDTO(createdProfile));
    }

    @PutMapping("/customer-profiles")
    @ApiMessage("Update customer profile")
    public ResponseEntity<ResCustomerProfileDTO> updateCustomerProfile(@RequestBody CustomerProfile customerProfile)
            throws IdInvalidException {
        CustomerProfile updatedProfile = customerProfileService.updateCustomerProfile(customerProfile);
        return ResponseEntity.ok(customerProfileMapper.convertToDTO(updatedProfile));
    }

    @GetMapping("/customer-profiles")
    @ApiMessage("Get all customer profiles")
    public ResponseEntity<ResultPaginationDTO> getAllCustomerProfiles(
            @Filter Specification<CustomerProfile> spec, Pageable pageable) {
        ResultPaginationDTO result = customerProfileService.getAllCustomerProfiles(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customer-profiles/{id}")
    @ApiMessage("Get customer profile by id")
    public ResponseEntity<ResCustomerProfileDTO> getCustomerProfileById(@PathVariable("id") Long id)
            throws IdInvalidException {
        CustomerProfile profile = customerProfileService.getCustomerProfileById(id);
        if (profile == null) {
            throw new IdInvalidException("Customer profile not found with id: " + id);
        }
        return ResponseEntity.ok(customerProfileMapper.convertToDTO(profile));
    }

    @GetMapping("/customer-profiles/user/{userId}")
    @ApiMessage("Get customer profile by user id")
    public ResponseEntity<ResCustomerProfileDTO> getCustomerProfileByUserId(@PathVariable("userId") Long userId)
            throws IdInvalidException {
        CustomerProfile profile = customerProfileService.getCustomerProfileByUserId(userId);
        if (profile == null) {
            throw new IdInvalidException("Customer profile not found for user id: " + userId);
        }
        return ResponseEntity.ok(customerProfileMapper.convertToDTO(profile));
    }

    @DeleteMapping("/customer-profiles/{id}")
    @ApiMessage("Delete customer profile by id")
    public ResponseEntity<Void> deleteCustomerProfile(@PathVariable("id") Long id) throws IdInvalidException {
        CustomerProfile profile = customerProfileService.getCustomerProfileById(id);
        if (profile == null) {
            throw new IdInvalidException("Customer profile not found with id: " + id);
        }
        customerProfileService.deleteCustomerProfile(id);
        return ResponseEntity.ok().body(null);
    }

    // Endpoints for current logged-in user

    @GetMapping("/customer-profile/me")
    @ApiMessage("Get current user's customer profile")
    public ResponseEntity<ResCustomerProfileDTO> getCurrentUserProfile() throws IdInvalidException {
        CustomerProfile profile = customerProfileService.getCurrentUserProfile();
        if (profile == null) {
            throw new IdInvalidException("Customer profile not found for current user");
        }
        return ResponseEntity.ok(customerProfileMapper.convertToDTO(profile));
    }

    @PostMapping("/customer-profile/me")
    @ApiMessage("Create customer profile for current user")
    public ResponseEntity<ResCustomerProfileDTO> createCurrentUserProfile(@RequestBody CustomerProfile customerProfile)
            throws IdInvalidException {
        CustomerProfile createdProfile = customerProfileService.createCurrentUserProfile(customerProfile);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerProfileMapper.convertToDTO(createdProfile));
    }

    @PutMapping("/customer-profile/me")
    @ApiMessage("Update current user's customer profile")
    public ResponseEntity<ResCustomerProfileDTO> updateCurrentUserProfile(@RequestBody CustomerProfile customerProfile)
            throws IdInvalidException {
        CustomerProfile updatedProfile = customerProfileService.updateCurrentUserProfile(customerProfile);
        return ResponseEntity.ok(customerProfileMapper.convertToDTO(updatedProfile));
    }

    @DeleteMapping("/customer-profile/me")
    @ApiMessage("Delete current user's customer profile")
    public ResponseEntity<Void> deleteCurrentUserProfile() throws IdInvalidException {
        customerProfileService.deleteCurrentUserProfile();
        return ResponseEntity.ok().body(null);
    }
}
