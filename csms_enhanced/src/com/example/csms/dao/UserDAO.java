package com.example.csms.dao;

import com.example.csms.model.User;
import java.util.Optional;

public interface UserDAO {
    Optional<User> findByUsername(String username);
    // Potentiellement : void addUser(User user); void updateUser(User user); etc.
}