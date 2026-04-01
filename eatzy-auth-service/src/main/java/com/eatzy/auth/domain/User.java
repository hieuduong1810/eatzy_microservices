package com.eatzy.auth.domain;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.*;

import com.eatzy.common.util.SecurityUtils;
import com.eatzy.common.constant.GenderEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @NotBlank(message = "Password không được để trống")
    private String password;

    @NotBlank(message = "Email không được để trống")
    private String email;
    private String phoneNumber;
    private Integer age;

    @Enumerated(EnumType.STRING)
    private GenderEnum gender;

    private String address;
    private Boolean isActive;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String refreshToken;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    // Role relationship
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    // Customer Profile (One-to-One)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CustomerProfile customerProfile;

    // Driver Profile (One-to-One)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DriverProfile driverProfile;


    // Addresses (One-to-Many for customers)
    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private List<Address> addresses;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtils.getCurrentUserLogin().isPresent()
                ? SecurityUtils.getCurrentUserLogin().get()
                : "";
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtils.getCurrentUserLogin().isPresent()
                ? SecurityUtils.getCurrentUserLogin().get()
                : "";
        this.updatedAt = Instant.now();
    }
}
