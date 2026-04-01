package com.eatzy.auth.domain.res.user;

import java.time.Instant;
import com.eatzy.common.constant.GenderEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResUpdateUserDTO {
    private long id;
    private String name;
    private GenderEnum gender;
    private String address;
    private Integer age;
    private Instant updatedAt;
    private Role role;

    @Getter
    @Setter
    public static class Role {
        private long id;
        private String name;
    }
}
