package com.gym.gym_management.service;

import com.gym.gym_management.authentication.RegisterRequest;
import com.gym.gym_management.model.Role;
import com.gym.gym_management.model.User;
import com.gym.gym_management.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(IUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if(existingUser.isPresent()){
            throw new RuntimeException("El usuario ya existe");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Role role;
        try {
            // Convertimos el String a mayúsculas para que coincida con los nombres del enum
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("El rol ingresado no es válido");
        }
        user.setRole(role);
        userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

}
