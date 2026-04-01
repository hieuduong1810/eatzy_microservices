package com.eatzy.auth.domain.res.driverProfile;

import java.math.BigDecimal;

import com.eatzy.common.constant.GenderEnum;
import com.eatzy.common.constant.StatusEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResDriverProfileDTO {
    private Long id;
    private UserDriver user;
    private String vehicleDetails;
    private String status;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private BigDecimal averageRating;
    private BigDecimal codLimit;
    private Integer completedTrips;

    // National ID
    @JsonProperty("national_id_front")
    private String nationalIdFront;

    @JsonProperty("national_id_back")
    private String nationalIdBack;

    @JsonProperty("national_id_number")
    private String nationalIdNumber;

    @JsonProperty("national_id_status")
    private StatusEnum nationalIdStatus;

    @JsonProperty("national_id_rejection_reason")
    private String nationalIdRejectionReason;

    // Profile Photo
    @JsonProperty("profile_photo")
    private String profilePhoto;

    @JsonProperty("profile_photo_status")
    private StatusEnum profilePhotoStatus;

    @JsonProperty("profile_photo_rejection_reason")
    private String profilePhotoRejectionReason;

    // Driver License
    @JsonProperty("driver_license_image")
    private String driverLicenseImage;

    @JsonProperty("driver_license_number")
    private String driverLicenseNumber;

    @JsonProperty("driver_license_class")
    private String driverLicenseClass;

    @JsonProperty("driver_license_expiry")
    private java.time.LocalDate driverLicenseExpiry;

    @JsonProperty("driver_license_status")
    private StatusEnum driverLicenseStatus;

    @JsonProperty("driver_license_rejection_reason")
    private String driverLicenseRejectionReason;

    // Bank Account & Tax Info
    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("bank_branch")
    private String bankBranch;

    @JsonProperty("bank_account_holder")
    private String bankAccountHolder;

    @JsonProperty("bank_account_number")
    private String bankAccountNumber;

    @JsonProperty("tax_code")
    private String taxCode;

    @JsonProperty("bank_account_image")
    private String bankAccountImage;

    @JsonProperty("bank_account_status")
    private StatusEnum bankAccountStatus;

    @JsonProperty("bank_account_rejection_reason")
    private String bankAccountRejectionReason;

    // Vehicle Information
    @JsonProperty("vehicle_type")
    private String vehicleType;

    @JsonProperty("vehicle_brand")
    private String vehicleBrand;

    @JsonProperty("vehicle_model")
    private String vehicleModel;

    @JsonProperty("vehicle_license_plate")
    private String vehicleLicensePlate;

    @JsonProperty("vehicle_year")
    private Integer vehicleYear;

    // Vehicle Registration
    @JsonProperty("vehicle_registration_image")
    private String vehicleRegistrationImage;

    @JsonProperty("vehicle_registration_status")
    private StatusEnum vehicleRegistrationStatus;

    @JsonProperty("vehicle_registration_rejection_reason")
    private String vehicleRegistrationRejectionReason;

    // Vehicle Insurance
    @JsonProperty("vehicle_insurance_image")
    private String vehicleInsuranceImage;

    @JsonProperty("vehicle_insurance_expiry")
    private java.time.LocalDate vehicleInsuranceExpiry;

    @JsonProperty("vehicle_insurance_status")
    private StatusEnum vehicleInsuranceStatus;

    @JsonProperty("vehicle_insurance_rejection_reason")
    private String vehicleInsuranceRejectionReason;

    // Vehicle Photo
    @JsonProperty("vehicle_photo")
    private String vehiclePhoto;

    @JsonProperty("vehicle_photo_status")
    private StatusEnum vehiclePhotoStatus;

    @JsonProperty("vehicle_photo_rejection_reason")
    private String vehiclePhotoRejectionReason;

    // Criminal Record
    @JsonProperty("criminal_record_image")
    private String criminalRecordImage;

    @JsonProperty("criminal_record_number")
    private String criminalRecordNumber;

    @JsonProperty("criminal_record_issue_date")
    private java.time.LocalDate criminalRecordIssueDate;

    @JsonProperty("criminal_record_status")
    private StatusEnum criminalRecordStatus;

    @JsonProperty("criminal_record_rejection_reason")
    private String criminalRecordRejectionReason;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDriver {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private Boolean isActive;
        private GenderEnum gender;
        private Integer age;
        private String address;
    }
}
