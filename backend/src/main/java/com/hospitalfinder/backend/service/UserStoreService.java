package com.hospitalfinder.backend.service;

import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.Role;

public interface UserStoreService {
    boolean existsByEmail(String email);

    UserData findByEmail(String email);

    UserData findById(Long id);

    UserData createUser(String name, String email, String phone, String password, Role role);

    UserData updateUser(String email, String name, String phone, Integer age, String gender, String password);
}
