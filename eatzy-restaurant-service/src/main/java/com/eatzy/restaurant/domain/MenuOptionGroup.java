package com.eatzy.restaurant.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "menu_option_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuOptionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dish_id")
    private Dish dish;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "min_choices")
    private Integer minChoices;

    @Column(name = "max_choices")
    private Integer maxChoices;

    @OneToMany(mappedBy = "menuOptionGroup", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<MenuOption> menuOptions;
}
