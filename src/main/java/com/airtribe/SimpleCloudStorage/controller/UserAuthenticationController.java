package com.airtribe.SimpleCloudStorage.controller;

import com.airtribe.SimpleCloudStorage.dto.AuthenticateUser;
import com.airtribe.SimpleCloudStorage.dto.AuthenticationResponse;
import com.airtribe.SimpleCloudStorage.dto.RegisterUser;
import com.airtribe.SimpleCloudStorage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthenticationController {


    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUser(@RequestBody RegisterUser registeredUser){

        return ResponseEntity.ok(userService.registerUser(registeredUser));

    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginUser(@RequestBody AuthenticateUser loggedUser){
        return ResponseEntity.ok(userService.authenticate(loggedUser));

    }
}
