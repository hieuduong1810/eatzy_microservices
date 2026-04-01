package com.eatzy.auth.mapper;

import org.springframework.stereotype.Component;

import com.eatzy.auth.domain.DriverProfile;
import com.eatzy.auth.domain.res.driverProfile.ResDriverProfileDTO;

@Component
public class DriverProfileMapper {

    public ResDriverProfileDTO convertToResDriverProfileDTO(DriverProfile profile) {
        if (profile == null) return null;
        ResDriverProfileDTO dto = new ResDriverProfileDTO();

        dto.setId(profile.getId());
        dto.setVehicleDetails(profile.getVehicleDetails());
        dto.setStatus(profile.getStatus());
        dto.setCurrentLatitude(profile.getCurrentLatitude());
        dto.setCurrentLongitude(profile.getCurrentLongitude());
        dto.setAverageRating(profile.getAverageRating());
        dto.setCodLimit(profile.getCodLimit());
        dto.setCompletedTrips(profile.getCompletedTrips());

        if (profile.getUser() != null) {
            ResDriverProfileDTO.UserDriver userDriver = new ResDriverProfileDTO.UserDriver();
            userDriver.setId(profile.getUser().getId());
            userDriver.setName(profile.getUser().getName());
            userDriver.setEmail(profile.getUser().getEmail());
            userDriver.setPhoneNumber(profile.getUser().getPhoneNumber());
            userDriver.setIsActive(profile.getUser().getIsActive());
            userDriver.setGender(profile.getUser().getGender());
            userDriver.setAge(profile.getUser().getAge());
            userDriver.setAddress(profile.getUser().getAddress());
            dto.setUser(userDriver);
        }

        dto.setNationalIdFront(profile.getNationalIdFront());
        dto.setNationalIdBack(profile.getNationalIdBack());
        dto.setNationalIdNumber(profile.getNationalIdNumber());
        dto.setNationalIdStatus(profile.getNationalIdStatus());
        dto.setNationalIdRejectionReason(profile.getNationalIdRejectionReason());

        dto.setProfilePhoto(profile.getProfilePhoto());
        dto.setProfilePhotoStatus(profile.getProfilePhotoStatus());
        dto.setProfilePhotoRejectionReason(profile.getProfilePhotoRejectionReason());

        dto.setDriverLicenseImage(profile.getDriverLicenseImage());
        dto.setDriverLicenseNumber(profile.getDriverLicenseNumber());
        dto.setDriverLicenseClass(profile.getDriverLicenseClass());
        dto.setDriverLicenseExpiry(profile.getDriverLicenseExpiry());
        dto.setDriverLicenseStatus(profile.getDriverLicenseStatus());
        dto.setDriverLicenseRejectionReason(profile.getDriverLicenseRejectionReason());

        dto.setBankName(profile.getBankName());
        dto.setBankBranch(profile.getBankBranch());
        dto.setBankAccountHolder(profile.getBankAccountHolder());
        dto.setBankAccountNumber(profile.getBankAccountNumber());
        dto.setTaxCode(profile.getTaxCode());
        dto.setBankAccountImage(profile.getBankAccountImage());
        dto.setBankAccountStatus(profile.getBankAccountStatus());
        dto.setBankAccountRejectionReason(profile.getBankAccountRejectionReason());

        dto.setVehicleType(profile.getVehicleType());
        dto.setVehicleBrand(profile.getVehicleBrand());
        dto.setVehicleModel(profile.getVehicleModel());
        dto.setVehicleLicensePlate(profile.getVehicleLicensePlate());
        dto.setVehicleYear(profile.getVehicleYear());

        dto.setVehicleRegistrationImage(profile.getVehicleRegistrationImage());
        dto.setVehicleRegistrationStatus(profile.getVehicleRegistrationStatus());
        dto.setVehicleRegistrationRejectionReason(profile.getVehicleRegistrationRejectionReason());

        dto.setVehicleInsuranceImage(profile.getVehicleInsuranceImage());
        dto.setVehicleInsuranceExpiry(profile.getVehicleInsuranceExpiry());
        dto.setVehicleInsuranceStatus(profile.getVehicleInsuranceStatus());
        dto.setVehicleInsuranceRejectionReason(profile.getVehicleInsuranceRejectionReason());

        dto.setVehiclePhoto(profile.getVehiclePhoto());
        dto.setVehiclePhotoStatus(profile.getVehiclePhotoStatus());
        dto.setVehiclePhotoRejectionReason(profile.getVehiclePhotoRejectionReason());

        dto.setCriminalRecordImage(profile.getCriminalRecordImage());
        dto.setCriminalRecordNumber(profile.getCriminalRecordNumber());
        dto.setCriminalRecordIssueDate(profile.getCriminalRecordIssueDate());
        dto.setCriminalRecordStatus(profile.getCriminalRecordStatus());
        dto.setCriminalRecordRejectionReason(profile.getCriminalRecordRejectionReason());

        return dto;
    }
}
