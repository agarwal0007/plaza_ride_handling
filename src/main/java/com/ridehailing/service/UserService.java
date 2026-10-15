package com.ridehailing.service;

import com.ridehailing.model.User;
import com.ridehailing.repo.UserRepository;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String userId, String name) {
        User user = new User(userId, name);
        userRepository.save(user);
        return user;
    }
}
