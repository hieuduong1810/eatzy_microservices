package com.eatzy.systemconfig.domain;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String configValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "last_updated_by")
    private Long lastUpdatedBy;

    private Instant updatedAt;
}
