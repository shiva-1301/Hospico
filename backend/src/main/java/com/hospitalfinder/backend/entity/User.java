package com.hospitalfinder.backend.entity;

import lombok.Getter;
import lombok.Setter;

public class User {
    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String email;
    @Getter
    @Setter
    private String phone;
    @Getter
    @Setter
    private String password;

    @Getter
    @Setter
    private Integer age;
    @Getter
    @Setter
    private String gender;

    @Getter
    @Setter
    private Role role;
    // getters and setters
}
