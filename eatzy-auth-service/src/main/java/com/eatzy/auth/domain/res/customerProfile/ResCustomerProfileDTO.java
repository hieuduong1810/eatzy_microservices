package com.eatzy.auth.domain.res.customerProfile;

import java.time.LocalDate;

import com.eatzy.common.constant.GenderEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCustomerProfileDTO {
    private Long id;
    private UserCustomer user;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    private String hometown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCustomer {
        private Long id;
        private String name;
        private String phoneNumber;
        private Boolean isActive;
        private GenderEnum gender;
        private Integer age;
        private String address;
    }
}
