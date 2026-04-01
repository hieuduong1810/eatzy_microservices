package com.eatzy.auth.domain.res.user;

import java.time.Instant;
import com.eatzy.common.constant.GenderEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResCreateUserDTO {
    private long id;
    private String name;
    private String email;
    private GenderEnum gender;
    private String address;
    private Integer age;
    private Instant createdAt;
    private Role role;

    @Getter
    @Setter
    public static class Role {
        private long id;
        private String name;
    }
}
