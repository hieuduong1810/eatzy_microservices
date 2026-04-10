package com.eatzy.auth.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.eatzy.auth.domain.Role;
import com.eatzy.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.domain.res.user.ResCreateUserDTO;
import com.eatzy.auth.domain.res.user.ResUpdateUserDTO;
import com.eatzy.auth.domain.res.user.ResUserDTO;
import com.eatzy.auth.repository.UserRepository;
import com.eatzy.auth.event.UserStatusLocalEvent;
import com.eatzy.auth.mapper.UserMapper;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    public UserService(
            UserRepository userRepository,
            RoleService roleService,
            ApplicationEventPublisher eventPublisher,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
    }

    @Transactional
    public User handleCreateUser(User user) {
        // Set user as inactive until email verification
        user.setIsActive(false);

        // check role
        if (user.getRole() != null) {
            Role role = this.roleService.getRoleById(user.getRole().getId());
            user.setRole(role != null ? role : null);
        }

        User savedUser = this.userRepository.save(user);

        // TODO: Send verification email with OTP
        // EmailVerificationService will be added later

        return savedUser;
    }

    @Transactional
    public void handleDeleteUser(Long id) {
        this.userRepository.deleteById(id);
    }

    @Transactional
    public User getUserById(Long id) {
        Optional<User> userOpt = this.userRepository.findById(id);
        return userOpt.orElse(null);
    }

    @Transactional
    public ResultPaginationDTO getAllUsers(Specification<User> spec, Pageable pageable) {
        Page<User> page = this.userRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);

        List<ResUserDTO> listUser = page.getContent().stream().map(userMapper::convertToResUserDTO)
                .collect(Collectors.toList());
        result.setResult(listUser);
        return result;
    }

    @Transactional
    public User handleUpdateUser(User user) {
        User currentUser = this.getUserById(user.getId());
        if (currentUser != null) {
            currentUser.setName(user.getName());
            currentUser.setAddress(user.getAddress());
            currentUser.setAge(user.getAge());
            currentUser.setGender(user.getGender());
            if (user.getRole() != null) {
                Role role = this.roleService.getRoleById(user.getRole().getId());
                currentUser.setRole(role != null ? role : null);
            }
            currentUser = this.userRepository.save(currentUser);
        }
        return currentUser;
    }

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public User getUserByRoleName(String roleName) {
        return this.userRepository.findFirstByRoleName(roleName);
    }

    public boolean checkEmailExists(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenAndEmail(String refreshToken, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(refreshToken, email);
    }

    /**
     * Set user active status.
     * Notifies observers (via Spring Events -> Kafka) about the status change.
     */
    @Transactional
    public User setUserActiveStatus(Long userId, Boolean isActive) {
        User user = this.getUserById(userId);
        if (user == null) {
            return null;
        }

        user.setIsActive(isActive);

        // If changing to inactive, force logout
        if (Boolean.FALSE.equals(isActive)) {
            user.setRefreshToken(null);
        }

        User savedUser = this.userRepository.save(user);

        // Observer Pattern: Subject publishes event to Observers
        String roleName = (savedUser.getRole() != null) ? savedUser.getRole().getName() : "USER";
        this.eventPublisher.publishEvent(new UserStatusLocalEvent(this, savedUser.getId(), roleName, isActive));

        return savedUser;
    }
}
