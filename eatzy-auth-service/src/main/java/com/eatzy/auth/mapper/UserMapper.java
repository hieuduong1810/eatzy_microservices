package com.eatzy.auth.mapper;

import org.springframework.stereotype.Component;

import com.eatzy.auth.domain.User;
import com.eatzy.auth.domain.res.user.ResCreateUserDTO;
import com.eatzy.auth.domain.res.user.ResUpdateUserDTO;
import com.eatzy.auth.domain.res.user.ResUserDTO;

@Component
public class UserMapper {

    public ResCreateUserDTO convertToResCreateUserDTO(User user) {
        if (user == null) return null;
        ResCreateUserDTO resUserDTO = new ResCreateUserDTO();
        ResCreateUserDTO.Role role = new ResCreateUserDTO.Role();
        resUserDTO.setId(user.getId());
        resUserDTO.setName(user.getName());
        resUserDTO.setEmail(user.getEmail());
        resUserDTO.setGender(user.getGender());
        resUserDTO.setAddress(user.getAddress());
        resUserDTO.setAge(user.getAge());
        resUserDTO.setCreatedAt(user.getCreatedAt());
        if (user.getRole() != null) {
            role.setId(user.getRole().getId());
            role.setName(user.getRole().getName());
            resUserDTO.setRole(role);
        }
        return resUserDTO;
    }

    public ResUserDTO convertToResUserDTO(User user) {
        if (user == null) return null;
        ResUserDTO resUserDTO = new ResUserDTO();
        ResUserDTO.Role role = new ResUserDTO.Role();
        resUserDTO.setId(user.getId());
        resUserDTO.setName(user.getName());
        resUserDTO.setEmail(user.getEmail());
        resUserDTO.setGender(user.getGender());
        resUserDTO.setAddress(user.getAddress());
        resUserDTO.setAge(user.getAge());
        resUserDTO.setCreatedAt(user.getCreatedAt());
        resUserDTO.setUpdatedAt(user.getUpdatedAt());
        if (user.getRole() != null) {
            role.setId(user.getRole().getId());
            role.setName(user.getRole().getName());
            resUserDTO.setRole(role);
        }
        return resUserDTO;
    }

    public ResUpdateUserDTO convertToResUpdateUserDTO(User user) {
        if (user == null) return null;
        ResUpdateUserDTO resUserDTO = new ResUpdateUserDTO();
        ResUpdateUserDTO.Role role = new ResUpdateUserDTO.Role();
        resUserDTO.setId(user.getId());
        resUserDTO.setName(user.getName());
        resUserDTO.setGender(user.getGender());
        resUserDTO.setAddress(user.getAddress());
        resUserDTO.setAge(user.getAge());
        resUserDTO.setUpdatedAt(user.getUpdatedAt());
        if (user.getRole() != null) {
            role.setId(user.getRole().getId());
            role.setName(user.getRole().getName());
            resUserDTO.setRole(role);
        }
        return resUserDTO;
    }
}
