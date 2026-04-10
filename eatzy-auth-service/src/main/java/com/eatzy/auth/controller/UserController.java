package com.eatzy.auth.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.eatzy.auth.domain.User;
import com.eatzy.auth.domain.res.user.ResUpdateUserDTO;
import com.eatzy.auth.domain.res.user.ResUserDTO;
import com.eatzy.auth.service.UserService;
import com.eatzy.auth.mapper.UserMapper;
import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping
    @ApiMessage("Get all users")
    public ResponseEntity<ResultPaginationDTO> getAllUsers(
            @Filter Specification<User> spec, Pageable pageable) {
        return ResponseEntity.ok(this.userService.getAllUsers(spec, pageable));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get user by id")
    public ResponseEntity<ResUserDTO> getUserById(@PathVariable("id") Long id) throws IdInvalidException {
        User user = this.userService.getUserById(id);
        if (user == null) {
            throw new IdInvalidException("User id không tồn tại");
        }
        return ResponseEntity.ok(this.userMapper.convertToResUserDTO(user));
    }

    @GetMapping("/email/{email}")
    @ApiMessage("Get user by email")
    public ResponseEntity<ResUserDTO> getUserByEmail(@PathVariable("email") String email) throws IdInvalidException {
        User user = this.userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User email không tồn tại");
        }
        return ResponseEntity.ok(this.userMapper.convertToResUserDTO(user));
    }

    @PutMapping
    @ApiMessage("Update user")
    public ResponseEntity<ResUpdateUserDTO> updateUser(@Valid @RequestBody User user) throws IdInvalidException {
        User existingUser = this.userService.getUserById(user.getId());
        if (existingUser == null) {
            throw new IdInvalidException("User id không tồn tại");
        }
        User updatedUser = this.userService.handleUpdateUser(user);
        return ResponseEntity.ok(this.userMapper.convertToResUpdateUserDTO(updatedUser));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) throws IdInvalidException {
        User user = this.userService.getUserById(id);
        if (user == null) {
            throw new IdInvalidException("User id không tồn tại");
        }
        this.userService.handleDeleteUser(id);
        return ResponseEntity.ok(null);
    }

    // This endpoint triggers the Observer Pattern -> Kafka event
    @PatchMapping("/{id}/status")
    @ApiMessage("Update user active status")
    public ResponseEntity<ResUserDTO> updateUserStatus(
            @PathVariable("id") Long id,
            @RequestParam("isActive") Boolean isActive) throws IdInvalidException {
        User existingUser = this.userService.getUserById(id);
        if (existingUser == null) {
            throw new IdInvalidException("User id không tồn tại");
        }
        User updatedUser = this.userService.setUserActiveStatus(id, isActive);
        return ResponseEntity.ok(this.userMapper.convertToResUserDTO(updatedUser));
    }

    @GetMapping("/role/{roleName}")
    @ApiMessage("Get first user by role name")
    public ResponseEntity<ResUserDTO> getUserByRoleName(@PathVariable("roleName") String roleName) throws IdInvalidException {
        User user = this.userService.getUserByRoleName(roleName);
        if (user == null) {
            throw new IdInvalidException("User with role " + roleName + " không tồn tại");
        }
        return ResponseEntity.ok(this.userMapper.convertToResUserDTO(user));
    }
}
