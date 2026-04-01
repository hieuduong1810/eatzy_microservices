package com.eatzy.auth.domain.res.address;

import java.math.BigDecimal;

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
public class ResAddressDTO {
    private Long id;
    private UserCustomer customer;

    @JsonProperty("address_line")
    private String addressLine;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String label;

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
