package com.airtribe.SimpleCloudStorage.controller;

import com.airtribe.SimpleCloudStorage.dto.AuthenticateUser;
import com.airtribe.SimpleCloudStorage.dto.AuthenticationResponse;
import com.airtribe.SimpleCloudStorage.dto.RegisterUser;
import com.airtribe.SimpleCloudStorage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthenticationController {


    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterUser registeredUser){

        try {
            AuthenticationResponse response = userService.registerUser(registeredUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }

    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginUser(@RequestBody AuthenticateUser loggedUser){
        return ResponseEntity.ok(userService.authenticate(loggedUser));

    }
}
