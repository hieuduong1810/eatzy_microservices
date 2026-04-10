package com.eatzy.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import com.eatzy.auth.domain.DriverProfile;
import com.eatzy.auth.domain.res.driverProfile.ResDriverProfileDTO;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.service.DriverProfileService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1")
public class DriverProfileController {
    private final DriverProfileService driverProfileService;

    public DriverProfileController(DriverProfileService driverProfileService) {
        this.driverProfileService = driverProfileService;
    }

    @PostMapping("/driver-profiles")
    @ApiMessage("Create driver profile")
    public ResponseEntity<ResDriverProfileDTO> createDriverProfile(@RequestBody DriverProfile driverProfile)
            throws IdInvalidException {
        ResDriverProfileDTO createdProfile = driverProfileService.createDriverProfile(driverProfile);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
    }

    @PutMapping("/driver-profiles")
    @ApiMessage("Update driver profile")
    public ResponseEntity<ResDriverProfileDTO> updateDriverProfile(@RequestBody DriverProfile driverProfile)
            throws IdInvalidException {
        ResDriverProfileDTO updatedProfile = driverProfileService.updateDriverProfile(driverProfile);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/driver-profiles")
    @ApiMessage("Get all driver profiles")
    public ResponseEntity<ResultPaginationDTO> getAllDriverProfiles(
            @Filter Specification<DriverProfile> spec, Pageable pageable) {
        ResultPaginationDTO result = driverProfileService.getAllDriverProfiles(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/driver-profiles/{id}")
    @ApiMessage("Get driver profile by id")
    public ResponseEntity<ResDriverProfileDTO> getDriverProfileById(@PathVariable("id") Long id)
            throws IdInvalidException {
        ResDriverProfileDTO profile = driverProfileService.getDriverProfileById(id);
        if (profile == null) {
            throw new IdInvalidException("Driver profile not found with id: " + id);
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/driver-profiles/user/{userId}")
    @ApiMessage("Get driver profile by user id")
    public ResponseEntity<ResDriverProfileDTO> getDriverProfileByUserId(@PathVariable("userId") Long userId)
            throws IdInvalidException {
        ResDriverProfileDTO profile = driverProfileService.getDriverProfileByUserId(userId);
        if (profile == null) {
            throw new IdInvalidException("Driver profile not found for user id: " + userId);
        }
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/driver-profiles/{id}")
    @ApiMessage("Delete driver profile by id")
    public ResponseEntity<Void> deleteDriverProfile(@PathVariable("id") Long id) throws IdInvalidException {
        ResDriverProfileDTO profile = driverProfileService.getDriverProfileById(id);
        if (profile == null) {
            throw new IdInvalidException("Driver profile not found with id: " + id);
        }
        driverProfileService.deleteDriverProfile(id);
        return ResponseEntity.ok().body(null);
    }

    @PostMapping("/driver-profiles/go-online")
    @ApiMessage("Driver goes online (opens app)")
    public ResponseEntity<ResDriverProfileDTO> goOnline() throws IdInvalidException {
        ResDriverProfileDTO profile = driverProfileService.goOnline();
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/driver-profiles/go-offline")
    @ApiMessage("Driver goes offline (closes app)")
    public ResponseEntity<ResDriverProfileDTO> goOffline() throws IdInvalidException {
        ResDriverProfileDTO profile = driverProfileService.goOffline();
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/driver-profiles/my-profile/status")
    @ApiMessage("Get current driver's profile status")
    public ResponseEntity<Map<String, String>> getMyProfileStatus() throws IdInvalidException {
        DriverProfile profile = driverProfileService.getCurrentDriverProfile();
        Map<String, String> response = new HashMap<>();
        response.put("status", profile.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver-profiles/count")
    @ApiMessage("Count drivers by status")
    public ResponseEntity<Long> countDrivers(@RequestParam("status") String status) {
        return ResponseEntity.ok(driverProfileService.countDriversByStatus(status));
    }



    @PutMapping("/driver-profiles/user/{userId}/status")
    @ApiMessage("Update driver status by user id")
    public ResponseEntity<Void> updateDriverStatus(
            @PathVariable("userId") Long userId,
            @RequestParam("status") String status) throws IdInvalidException {
        driverProfileService.updateDriverProfileStatusByUserId(userId, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Increment driver's completed trips counter.
     * Called by order-service when order is delivered (matches eatzy_backend).
     */
    @PutMapping("/driver-profiles/user/{userId}/increment-completed-trips")
    @ApiMessage("Increment driver completed trips")
    public ResponseEntity<Void> incrementCompletedTrips(@PathVariable("userId") Long userId) throws IdInvalidException {
        driverProfileService.incrementCompletedTrips(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Validate driver IDs against SQL business rules (status = AVAILABLE, COD limit).
     * Matches eatzy_backend STEP 2: Query SQL to validate business rules.
     */
    @PostMapping("/driver-profiles/validate-drivers")
    @ApiMessage("Validate drivers by user IDs against business rules")
    public ResponseEntity<List<Long>> validateDriversByIds(
            @RequestBody List<Long> userIds,
            @RequestParam(value = "minCodLimit", required = false) BigDecimal minCodLimit) {
        List<Long> validDriverIds = driverProfileService.validateDriversByUserIds(userIds, minCodLimit);
        return ResponseEntity.ok(validDriverIds);
    }
}
