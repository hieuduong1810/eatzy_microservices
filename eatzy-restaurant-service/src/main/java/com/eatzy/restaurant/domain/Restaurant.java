package com.eatzy.restaurant.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thay User owner bằng ownerId (tham chiếu xuyên service qua ID)
    @Column(name = "owner_id")
    private Long ownerId;

    private String name;

    @Column(unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String description;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    private String contactPhone;
    private String status;

    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "one_star_count")
    private Integer oneStarCount;

    @Column(name = "two_star_count")
    private Integer twoStarCount;

    @Column(name = "three_star_count")
    private Integer threeStarCount;

    @Column(name = "four_star_count")
    private Integer fourStarCount;

    @Column(name = "five_star_count")
    private Integer fiveStarCount;

    @Column(columnDefinition = "TEXT")
    private String schedule;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<DishCategory> dishCategories;

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Dish> dishes;

    // TODO: orders, carts, vouchers, favorites, monthlyRevenueReports
    // thuoc domain Order/Cart/Voucher -> se giao tiep qua REST API khi co Order Service

    @ManyToMany
    @JsonIgnoreProperties("restaurants")
    @JoinTable(name = "restaurant_restaurant_type", joinColumns = @JoinColumn(name = "restaurant_id"), inverseJoinColumns = @JoinColumn(name = "restaurant_type_id"))
    private List<RestaurantType> restaurantTypes;
}