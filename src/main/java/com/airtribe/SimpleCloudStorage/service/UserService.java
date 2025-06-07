package com.airtribe.SimpleCloudStorage.service;


import com.airtribe.SimpleCloudStorage.config.JwtService;
import com.airtribe.SimpleCloudStorage.dto.AuthenticateUser;
import com.airtribe.SimpleCloudStorage.dto.AuthenticationResponse;
import com.airtribe.SimpleCloudStorage.dto.RegisterUser;
import com.airtribe.SimpleCloudStorage.entity.Users;
import com.airtribe.SimpleCloudStorage.enums.Role;
import com.airtribe.SimpleCloudStorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserService {


    private final UserRepository userRepository;
    private final JwtService jwtService; // Must be final for Lombok
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;


    public AuthenticationResponse registerUser(RegisterUser registeredUser) {
        Role role = registeredUser.getUsername().contains("admin")?Role.ADMIN:Role.USER;

        Users user = Users.builder()
                .username(registeredUser.getUsername())
                .email(registeredUser.getEmail())
                .password_hash(passwordEncoder.encode(registeredUser.getPassword()))
                .created_at(new Date())
                .isActive(true)
                .last_login(new Date())
                .role(role)
                .build();

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
    public AuthenticationResponse authenticate(AuthenticateUser request) {
            System.out.println("username:"+request.getUsername());
        System.out.println("password:"+request.getPassword());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
        }

    }
