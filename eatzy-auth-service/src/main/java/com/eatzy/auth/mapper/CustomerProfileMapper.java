package com.eatzy.auth.mapper;

import org.springframework.stereotype.Component;

import com.eatzy.auth.domain.CustomerProfile;
import com.eatzy.auth.domain.res.customerProfile.ResCustomerProfileDTO;

@Component
public class CustomerProfileMapper {

    public ResCustomerProfileDTO convertToDTO(CustomerProfile profile) {
        if (profile == null) {
            return null;
        }

        ResCustomerProfileDTO dto = new ResCustomerProfileDTO();
        dto.setId(profile.getId());
        dto.setDateOfBirth(profile.getDateOfBirth());
        dto.setHometown(profile.getHometown());

        if (profile.getUser() != null) {
            ResCustomerProfileDTO.UserCustomer userDTO = new ResCustomerProfileDTO.UserCustomer();
            userDTO.setId(profile.getUser().getId());
            userDTO.setName(profile.getUser().getName());
            userDTO.setPhoneNumber(profile.getUser().getPhoneNumber());
            userDTO.setIsActive(profile.getUser().getIsActive());
            userDTO.setGender(profile.getUser().getGender());
            userDTO.setAge(profile.getUser().getAge());
            userDTO.setAddress(profile.getUser().getAddress());
            dto.setUser(userDTO);
        }

        return dto;
    }
}
