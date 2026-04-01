package com.eatzy.auth.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatzy.auth.domain.DriverProfile;
import com.eatzy.auth.domain.User;
import com.eatzy.auth.domain.res.driverProfile.ResDriverProfileDTO;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.repository.DriverProfileRepository;
import com.eatzy.auth.mapper.DriverProfileMapper;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.util.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DriverProfileService {
    private final DriverProfileRepository driverProfileRepository;
    private final UserService userService;
    private final DriverProfileMapper driverProfileMapper;

    public DriverProfileService(DriverProfileRepository driverProfileRepository,
            UserService userService, DriverProfileMapper driverProfileMapper) {
        this.driverProfileRepository = driverProfileRepository;
        this.userService = userService;
        this.driverProfileMapper = driverProfileMapper;
    }

    public boolean existsByUserId(Long userId) {
        return driverProfileRepository.existsByUserId(userId);
    }

    private DriverProfile getDriverProfileEntityById(Long id) throws IdInvalidException {
        return this.driverProfileRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Driver profile not found with id: " + id));
    }

    public ResDriverProfileDTO getDriverProfileById(Long id) {
        Optional<DriverProfile> profileOpt = this.driverProfileRepository.findById(id);
        return profileOpt.map(driverProfileMapper::convertToResDriverProfileDTO).orElse(null);
    }

    public ResDriverProfileDTO getDriverProfileByUserId(Long userId) {
        Optional<DriverProfile> profileOpt = this.driverProfileRepository.findByUserId(userId);
        return profileOpt.map(driverProfileMapper::convertToResDriverProfileDTO).orElse(null);
    }

    public DriverProfile getCurrentDriverProfile() throws IdInvalidException {
        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("User is not logged in"));

        User currentUser = this.userService.handleGetUserByUsername(email);
        if (currentUser == null) {
            throw new IdInvalidException("User not found with email: " + email);
        }

        Optional<DriverProfile> profileOpt = this.driverProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            throw new IdInvalidException("No driver profile found for user: " + currentUser.getName());
        }

        return profileOpt.get();
    }

    public ResDriverProfileDTO getCurrentDriverProfileDTO() throws IdInvalidException {
        DriverProfile profile = getCurrentDriverProfile();
        return driverProfileMapper.convertToResDriverProfileDTO(profile);
    }

    public ResDriverProfileDTO createDriverProfile(DriverProfile driverProfile) throws IdInvalidException {
        // check user exists
        if (driverProfile.getUser() != null) {
            User user = this.userService.getUserById(driverProfile.getUser().getId());
            if (user == null) {
                throw new IdInvalidException("User not found with id: " + driverProfile.getUser().getId());
            }

            // check if profile already exists for this user
            if (this.existsByUserId(user.getId())) {
                throw new IdInvalidException("Driver profile already exists for user id: " + user.getId());
            }

            driverProfile.setUser(user);
        } else {
            throw new IdInvalidException("User is required");
        }

        DriverProfile savedProfile = driverProfileRepository.save(driverProfile);
        return driverProfileMapper.convertToResDriverProfileDTO(savedProfile);
    }

    public ResDriverProfileDTO updateDriverProfile(DriverProfile driverProfile) throws IdInvalidException {
        DriverProfile currentProfile = getDriverProfileEntityById(driverProfile.getId());

        // update basic fields
        if (driverProfile.getVehicleDetails() != null) {
            currentProfile.setVehicleDetails(driverProfile.getVehicleDetails());
        }
        if (driverProfile.getStatus() != null) {
            currentProfile.setStatus(driverProfile.getStatus());
        }
        if (driverProfile.getCurrentLatitude() != null) {
            currentProfile.setCurrentLatitude(driverProfile.getCurrentLatitude());
        }
        if (driverProfile.getCurrentLongitude() != null) {
            currentProfile.setCurrentLongitude(driverProfile.getCurrentLongitude());
        }
        if (driverProfile.getCodLimit() != null) {
            currentProfile.setCodLimit(driverProfile.getCodLimit());
        }

        // update documents and vehicle details...
        if (driverProfile.getNationalIdFront() != null)
            currentProfile.setNationalIdFront(driverProfile.getNationalIdFront());
        if (driverProfile.getNationalIdBack() != null)
            currentProfile.setNationalIdBack(driverProfile.getNationalIdBack());
        if (driverProfile.getNationalIdNumber() != null)
            currentProfile.setNationalIdNumber(driverProfile.getNationalIdNumber());
        if (driverProfile.getNationalIdStatus() != null)
            currentProfile.setNationalIdStatus(driverProfile.getNationalIdStatus());
        if (driverProfile.getNationalIdRejectionReason() != null)
            currentProfile.setNationalIdRejectionReason(driverProfile.getNationalIdRejectionReason());

        if (driverProfile.getProfilePhoto() != null)
            currentProfile.setProfilePhoto(driverProfile.getProfilePhoto());
        if (driverProfile.getProfilePhotoStatus() != null)
            currentProfile.setProfilePhotoStatus(driverProfile.getProfilePhotoStatus());
        if (driverProfile.getProfilePhotoRejectionReason() != null)
            currentProfile.setProfilePhotoRejectionReason(driverProfile.getProfilePhotoRejectionReason());

        if (driverProfile.getDriverLicenseImage() != null)
            currentProfile.setDriverLicenseImage(driverProfile.getDriverLicenseImage());
        if (driverProfile.getDriverLicenseNumber() != null)
            currentProfile.setDriverLicenseNumber(driverProfile.getDriverLicenseNumber());
        if (driverProfile.getDriverLicenseClass() != null)
            currentProfile.setDriverLicenseClass(driverProfile.getDriverLicenseClass());
        if (driverProfile.getDriverLicenseExpiry() != null)
            currentProfile.setDriverLicenseExpiry(driverProfile.getDriverLicenseExpiry());
        if (driverProfile.getDriverLicenseStatus() != null)
            currentProfile.setDriverLicenseStatus(driverProfile.getDriverLicenseStatus());
        if (driverProfile.getDriverLicenseRejectionReason() != null)
            currentProfile.setDriverLicenseRejectionReason(driverProfile.getDriverLicenseRejectionReason());

        if (driverProfile.getBankName() != null)
            currentProfile.setBankName(driverProfile.getBankName());
        if (driverProfile.getBankBranch() != null)
            currentProfile.setBankBranch(driverProfile.getBankBranch());
        if (driverProfile.getBankAccountHolder() != null)
            currentProfile.setBankAccountHolder(driverProfile.getBankAccountHolder());
        if (driverProfile.getBankAccountNumber() != null)
            currentProfile.setBankAccountNumber(driverProfile.getBankAccountNumber());
        if (driverProfile.getTaxCode() != null)
            currentProfile.setTaxCode(driverProfile.getTaxCode());
        if (driverProfile.getBankAccountImage() != null)
            currentProfile.setBankAccountImage(driverProfile.getBankAccountImage());
        if (driverProfile.getBankAccountStatus() != null)
            currentProfile.setBankAccountStatus(driverProfile.getBankAccountStatus());
        if (driverProfile.getBankAccountRejectionReason() != null)
            currentProfile.setBankAccountRejectionReason(driverProfile.getBankAccountRejectionReason());

        if (driverProfile.getVehicleType() != null)
            currentProfile.setVehicleType(driverProfile.getVehicleType());
        if (driverProfile.getVehicleBrand() != null)
            currentProfile.setVehicleBrand(driverProfile.getVehicleBrand());
        if (driverProfile.getVehicleModel() != null)
            currentProfile.setVehicleModel(driverProfile.getVehicleModel());
        if (driverProfile.getVehicleLicensePlate() != null)
            currentProfile.setVehicleLicensePlate(driverProfile.getVehicleLicensePlate());
        if (driverProfile.getVehicleYear() != null)
            currentProfile.setVehicleYear(driverProfile.getVehicleYear());

        if (driverProfile.getVehicleRegistrationImage() != null)
            currentProfile.setVehicleRegistrationImage(driverProfile.getVehicleRegistrationImage());
        if (driverProfile.getVehicleRegistrationStatus() != null)
            currentProfile.setVehicleRegistrationStatus(driverProfile.getVehicleRegistrationStatus());
        if (driverProfile.getVehicleRegistrationRejectionReason() != null)
            currentProfile.setVehicleRegistrationRejectionReason(driverProfile.getVehicleRegistrationRejectionReason());

        if (driverProfile.getVehicleInsuranceImage() != null)
            currentProfile.setVehicleInsuranceImage(driverProfile.getVehicleInsuranceImage());
        if (driverProfile.getVehicleInsuranceExpiry() != null)
            currentProfile.setVehicleInsuranceExpiry(driverProfile.getVehicleInsuranceExpiry());
        if (driverProfile.getVehicleInsuranceStatus() != null)
            currentProfile.setVehicleInsuranceStatus(driverProfile.getVehicleInsuranceStatus());
        if (driverProfile.getVehicleInsuranceRejectionReason() != null)
            currentProfile.setVehicleInsuranceRejectionReason(driverProfile.getVehicleInsuranceRejectionReason());

        if (driverProfile.getVehiclePhoto() != null)
            currentProfile.setVehiclePhoto(driverProfile.getVehiclePhoto());
        if (driverProfile.getVehiclePhotoStatus() != null)
            currentProfile.setVehiclePhotoStatus(driverProfile.getVehiclePhotoStatus());
        if (driverProfile.getVehiclePhotoRejectionReason() != null)
            currentProfile.setVehiclePhotoRejectionReason(driverProfile.getVehiclePhotoRejectionReason());

        if (driverProfile.getCriminalRecordImage() != null)
            currentProfile.setCriminalRecordImage(driverProfile.getCriminalRecordImage());
        if (driverProfile.getCriminalRecordNumber() != null)
            currentProfile.setCriminalRecordNumber(driverProfile.getCriminalRecordNumber());
        if (driverProfile.getCriminalRecordIssueDate() != null)
            currentProfile.setCriminalRecordIssueDate(driverProfile.getCriminalRecordIssueDate());
        if (driverProfile.getCriminalRecordStatus() != null)
            currentProfile.setCriminalRecordStatus(driverProfile.getCriminalRecordStatus());
        if (driverProfile.getCriminalRecordRejectionReason() != null)
            currentProfile.setCriminalRecordRejectionReason(driverProfile.getCriminalRecordRejectionReason());

        DriverProfile savedProfile = driverProfileRepository.save(currentProfile);
        return driverProfileMapper.convertToResDriverProfileDTO(savedProfile);
    }

    public void updateDriverProfileStatusByUserId(Long userId, String status) throws IdInvalidException {
        Optional<DriverProfile> profileOpt = driverProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            DriverProfile profile = profileOpt.get();
            profile.setStatus(status);
            driverProfileRepository.save(profile);
        } else {
            throw new IdInvalidException("Driver profile not found for user id: " + userId);
        }
    }

    @Transactional
    public ResDriverProfileDTO goOnline() throws IdInvalidException {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("User not authenticated"));

        User driver = this.userService.handleGetUserByUsername(currentUserEmail);
        if (driver == null) {
            throw new IdInvalidException("Driver not found with email: " + currentUserEmail);
        }

        Optional<DriverProfile> profileOpt = driverProfileRepository.findByUserId(driver.getId());
        if (!profileOpt.isPresent()) {
            throw new IdInvalidException("Driver profile not found for user id: " + driver.getId());
        }

        DriverProfile profile = profileOpt.get();

        if (profile.getCurrentLatitude() == null || profile.getCurrentLongitude() == null) {
            throw new IdInvalidException("Driver location must be set before going online");
        }

        profile.setStatus("AVAILABLE");
        DriverProfile savedProfile = driverProfileRepository.save(profile);

        log.info("🟢 Driver {} (ID: {}) is now ONLINE", driver.getName(), driver.getId());

        // We emit a kafka event or similar in actual microservice architecture to let
        // Orchestrator know driver is online.

        return driverProfileMapper.convertToResDriverProfileDTO(savedProfile);
    }

    public ResDriverProfileDTO goOffline() throws IdInvalidException {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("User not authenticated"));

        User driver = this.userService.handleGetUserByUsername(currentUserEmail);
        if (driver == null) {
            throw new IdInvalidException("Driver not found with email: " + currentUserEmail);
        }

        Optional<DriverProfile> profileOpt = driverProfileRepository.findByUserId(driver.getId());
        if (!profileOpt.isPresent()) {
            throw new IdInvalidException("Driver profile not found for user id: " + driver.getId());
        }

        DriverProfile profile = profileOpt.get();
        profile.setStatus("OFFLINE");
        DriverProfile savedProfile = driverProfileRepository.save(profile);
        return driverProfileMapper.convertToResDriverProfileDTO(savedProfile);
    }

    public void updateDriverLocation(Long userId, java.math.BigDecimal latitude, java.math.BigDecimal longitude)
            throws IdInvalidException {
        Optional<DriverProfile> profileOpt = driverProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            DriverProfile profile = profileOpt.get();
            profile.setCurrentLatitude(latitude);
            profile.setCurrentLongitude(longitude);
            driverProfileRepository.save(profile);
        } else {
            throw new IdInvalidException("Driver profile not found for user id: " + userId);
        }
    }

    public ResultPaginationDTO getAllDriverProfiles(Specification<DriverProfile> spec, Pageable pageable) {
        Page<DriverProfile> page = this.driverProfileRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);

        result.setResult(page.getContent().stream()
                .map(driverProfileMapper::convertToResDriverProfileDTO)
                .collect(java.util.stream.Collectors.toList()));

        return result;
    }

    public void deleteDriverProfile(Long id) {
        this.driverProfileRepository.deleteById(id);
    }

    public long countDriversByStatus(String status) {
        return this.driverProfileRepository.countByStatus(status);
    }
}
