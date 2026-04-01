package com.eatzy.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eatzy.auth.domain.User;
import com.eatzy.auth.domain.req.ReqLoginDTO;
import com.eatzy.auth.domain.res.ResLoginDTO;
import com.eatzy.auth.domain.res.user.ResCreateUserDTO;
import com.eatzy.auth.service.UserService;
import com.eatzy.auth.mapper.UserMapper;
import com.eatzy.auth.util.SecurityUtil;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.exception.IdInvalidException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${foodDelivery.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtil securityUtil,
            UserService userService, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/auth/login")
    @ApiMessage("User login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDTO) throws IdInvalidException {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO resLoginDTO = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(loginDTO.getUsername());
        if (currentUserDB != null) {
            if (currentUserDB.getIsActive() == null || !currentUserDB.getIsActive()) {
                throw new IdInvalidException(
                        "Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để xác thực.");
            }

            ResLoginDTO.UserLogin userLogin = resLoginDTO.new UserLogin(currentUserDB.getId(),
                    currentUserDB.getEmail(), currentUserDB.getName(), currentUserDB.getRole());
            resLoginDTO.setUser(userLogin);
        }

        String access_token = this.securityUtil.createAccessToken(authentication.getName(), resLoginDTO);
        resLoginDTO.setAccessToken(access_token);

        String refresh_token = this.securityUtil.createRefreshToken(loginDTO.getUsername(), resLoginDTO);
        this.userService.updateUserToken(refresh_token, loginDTO.getUsername());

        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, resCookies.toString()).body(resLoginDTO);
    }

    @GetMapping("/auth/account")
    @ApiMessage("fetch account")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() {
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        ResLoginDTO resLoginDTO = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(email);
        ResLoginDTO.UserLogin userLogin = resLoginDTO.new UserLogin();
        ResLoginDTO.UserGetAccount userGetAccount = resLoginDTO.new UserGetAccount();
        if (currentUserDB != null) {
            userLogin.setId(currentUserDB.getId());
            userLogin.setEmail(currentUserDB.getEmail());
            userLogin.setName(currentUserDB.getName());
            userLogin.setRole(currentUserDB.getRole());
            userGetAccount.setUser(userLogin);
        }
        return ResponseEntity.ok(userGetAccount);
    }

    @GetMapping("/auth/refresh")
    @ApiMessage("Get user by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token)
            throws IdInvalidException {
        if (refresh_token.equals("abc")) {
            throw new IdInvalidException("Bạn không có refresh token ở cookie");
        }

        Jwt decodedToken = this.securityUtil.checkValidRefreshToken(refresh_token);
        String email = decodedToken.getSubject();

        User currentUser = this.userService.handleGetUserByUsername(email);
        if (currentUser == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        if (currentUser.getRefreshToken() == null || !currentUser.getRefreshToken().equals(refresh_token)) {
            throw new IdInvalidException("Refresh token không hợp lệ");
        }

        ResLoginDTO resLoginDTO = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = resLoginDTO.new UserLogin(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getName(),
                currentUser.getRole());
        resLoginDTO.setUser(userLogin);

        String access_token = this.securityUtil.createAccessToken(email, resLoginDTO);
        resLoginDTO.setAccessToken(access_token);

        String new_refresh_token = this.securityUtil.createRefreshToken(email, resLoginDTO);
        this.userService.updateUserToken(new_refresh_token, email);

        ResponseCookie resCookies = ResponseCookie.from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, resCookies.toString()).body(resLoginDTO);
    }

    @PostMapping("/auth/logout")
    @ApiMessage("User logout")
    public ResponseEntity<Void> logout() throws IdInvalidException {
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        if (email.equals("")) {
            throw new IdInvalidException("Access Token không hợp lệ");
        }

        this.userService.updateUserToken(null, email);

        ResponseCookie deleteSpringCookie = ResponseCookie.from("refresh_token", null)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString()).body(null);
    }

    @PostMapping("/auth/register")
    @ApiMessage("User registration")
    public ResponseEntity<ResCreateUserDTO> createNewUser(@Valid @RequestBody User user)
            throws IdInvalidException {
        boolean isEmailExist = this.userService.checkEmailExists(user.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException("Email already exists: " + user.getEmail());
        }
        String encodedPassword = this.passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        User savedUser = this.userService.handleCreateUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.userMapper.convertToResCreateUserDTO(savedUser));
    }
}
