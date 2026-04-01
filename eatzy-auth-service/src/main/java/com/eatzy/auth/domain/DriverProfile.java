package com.eatzy.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.eatzy.common.constant.StatusEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String vehicleDetails;
    private String status;

    @Column(precision = 10, scale = 8)
    private BigDecimal currentLatitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal currentLongitude;

    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(precision = 10, scale = 2)
    private BigDecimal codLimit;

    private Integer completedTrips;

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

    @JsonProperty("profile_photo")
    private String profilePhoto;
    @JsonProperty("profile_photo_status")
    private StatusEnum profilePhotoStatus;
    @JsonProperty("profile_photo_rejection_reason")
    private String profilePhotoRejectionReason;

    @JsonProperty("driver_license_image")
    private String driverLicenseImage;
    @JsonProperty("driver_license_number")
    private String driverLicenseNumber;
    @JsonProperty("driver_license_class")
    private String driverLicenseClass;
    @JsonProperty("driver_license_expiry")
    private LocalDate driverLicenseExpiry;
    @JsonProperty("driver_license_status")
    private StatusEnum driverLicenseStatus;
    @JsonProperty("driver_license_rejection_reason")
    private String driverLicenseRejectionReason;

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

    @JsonProperty("vehicle_registration_image")
    private String vehicleRegistrationImage;
    @JsonProperty("vehicle_registration_status")
    private StatusEnum vehicleRegistrationStatus;
    @JsonProperty("vehicle_registration_rejection_reason")
    private String vehicleRegistrationRejectionReason;

    @JsonProperty("vehicle_insurance_image")
    private String vehicleInsuranceImage;
    @JsonProperty("vehicle_insurance_expiry")
    private LocalDate vehicleInsuranceExpiry;
    @JsonProperty("vehicle_insurance_status")
    private StatusEnum vehicleInsuranceStatus;
    @JsonProperty("vehicle_insurance_rejection_reason")
    private String vehicleInsuranceRejectionReason;

    @JsonProperty("vehicle_photo")
    private String vehiclePhoto;
    @JsonProperty("vehicle_photo_status")
    private StatusEnum vehiclePhotoStatus;
    @JsonProperty("vehicle_photo_rejection_reason")
    private String vehiclePhotoRejectionReason;

    @JsonProperty("criminal_record_image")
    private String criminalRecordImage;
    @JsonProperty("criminal_record_number")
    private String criminalRecordNumber;
    @JsonProperty("criminal_record_issue_date")
    private LocalDate criminalRecordIssueDate;
    @JsonProperty("criminal_record_status")
    private StatusEnum criminalRecordStatus;
    @JsonProperty("criminal_record_rejection_reason")
    private String criminalRecordRejectionReason;
}
