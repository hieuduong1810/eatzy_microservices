package com.eatzy.auth.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.auth.domain.CustomerProfile;
import com.eatzy.auth.domain.User;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.domain.res.customerProfile.ResCustomerProfileDTO;
import com.eatzy.auth.repository.CustomerProfileRepository;
import com.eatzy.auth.mapper.CustomerProfileMapper;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class CustomerProfileService {
    private final CustomerProfileRepository customerProfileRepository;
    private final UserService userService;
    private final CustomerProfileMapper customerProfileMapper;

    public CustomerProfileService(CustomerProfileRepository customerProfileRepository, UserService userService, CustomerProfileMapper customerProfileMapper) {
        this.customerProfileRepository = customerProfileRepository;
        this.userService = userService;
        this.customerProfileMapper = customerProfileMapper;
    }

    public boolean existsByUserId(Long userId) {
        return customerProfileRepository.existsByUserId(userId);
    }

    public CustomerProfile getCustomerProfileById(Long id) {
        Optional<CustomerProfile> profileOpt = this.customerProfileRepository.findById(id);
        return profileOpt.orElse(null);
    }

    public CustomerProfile getCustomerProfileByUserId(Long userId) {
        Optional<CustomerProfile> profileOpt = this.customerProfileRepository.findByUserId(userId);
        return profileOpt.orElse(null);
    }

    public CustomerProfile createCustomerProfile(CustomerProfile customerProfile) throws IdInvalidException {
        // check user exists
        if (customerProfile.getUser() != null) {
            User user = this.userService.getUserById(customerProfile.getUser().getId());
            if (user == null) {
                throw new IdInvalidException("User not found with id: " + customerProfile.getUser().getId());
            }

            // check if profile already exists for this user
            if (this.existsByUserId(user.getId())) {
                throw new IdInvalidException("Customer profile already exists for user id: " + user.getId());
            }

            customerProfile.setUser(user);
        } else {
            throw new IdInvalidException("User is required");
        }

        return customerProfileRepository.save(customerProfile);
    }

    public CustomerProfile updateCustomerProfile(CustomerProfile customerProfile) throws IdInvalidException {
        // check id
        CustomerProfile currentProfile = getCustomerProfileById(customerProfile.getId());
        if (currentProfile == null) {
            throw new IdInvalidException("Customer profile not found with id: " + customerProfile.getId());
        }

        // update fields
        if (customerProfile.getDateOfBirth() != null) {
            currentProfile.setDateOfBirth(customerProfile.getDateOfBirth());
        }
        if (customerProfile.getHometown() != null) {
            currentProfile.setHometown(customerProfile.getHometown());
        }

        return customerProfileRepository.save(currentProfile);
    }

    public ResultPaginationDTO getAllCustomerProfiles(Specification<CustomerProfile> spec, Pageable pageable) {
        Page<CustomerProfile> page = this.customerProfileRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);

        List<ResCustomerProfileDTO> dtoList = page.getContent().stream()
                .map(customerProfileMapper::convertToDTO)
                .collect(Collectors.toList());
        result.setResult(dtoList);
        return result;
    }

    public void deleteCustomerProfile(Long id) {
        this.customerProfileRepository.deleteById(id);
    }

    // Methods for current logged-in user

    public CustomerProfile getCurrentUserProfile() throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        return this.getCustomerProfileByUserId(user.getId());
    }

    public CustomerProfile createCurrentUserProfile(CustomerProfile customerProfile) throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        // Check if profile already exists for this user
        if (this.existsByUserId(user.getId())) {
            throw new IdInvalidException("Customer profile already exists for the current user");
        }

        customerProfile.setUser(user);
        return customerProfileRepository.save(customerProfile);
    }

    public CustomerProfile updateCurrentUserProfile(CustomerProfile customerProfile) throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        CustomerProfile currentProfile = this.getCustomerProfileByUserId(user.getId());
        if (currentProfile == null) {
            throw new IdInvalidException("Customer profile not found for the current user");
        }

        // Update fields
        if (customerProfile.getDateOfBirth() != null) {
            currentProfile.setDateOfBirth(customerProfile.getDateOfBirth());
        }
        if (customerProfile.getHometown() != null) {
            currentProfile.setHometown(customerProfile.getHometown());
        }

        return customerProfileRepository.save(currentProfile);
    }

    public void deleteCurrentUserProfile() throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        CustomerProfile currentProfile = this.getCustomerProfileByUserId(user.getId());
        if (currentProfile == null) {
            throw new IdInvalidException("Customer profile not found for the current user");
        }

        this.customerProfileRepository.deleteById(currentProfile.getId());
    }
}
