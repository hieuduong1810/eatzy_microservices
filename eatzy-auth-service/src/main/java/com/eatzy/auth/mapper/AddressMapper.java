package com.eatzy.auth.mapper;

import org.springframework.stereotype.Component;

import com.eatzy.auth.domain.Address;
import com.eatzy.auth.domain.res.address.ResAddressDTO;

@Component
public class AddressMapper {

    public ResAddressDTO convertToDTO(Address address) {
        if (address == null) {
            return null;
        }

        ResAddressDTO dto = new ResAddressDTO();
        dto.setId(address.getId());
        dto.setAddressLine(address.getAddressLine());
        dto.setLatitude(address.getLatitude());
        dto.setLongitude(address.getLongitude());
        dto.setLabel(address.getLabel());

        if (address.getCustomer() != null) {
            ResAddressDTO.UserCustomer customerDTO = new ResAddressDTO.UserCustomer();
            customerDTO.setId(address.getCustomer().getId());
            customerDTO.setName(address.getCustomer().getName());
            customerDTO.setPhoneNumber(address.getCustomer().getPhoneNumber());
            customerDTO.setIsActive(address.getCustomer().getIsActive());
            customerDTO.setGender(address.getCustomer().getGender());
            customerDTO.setAge(address.getCustomer().getAge());
            customerDTO.setAddress(address.getCustomer().getAddress());
            dto.setCustomer(customerDTO);
        }

        return dto;
    }
}
