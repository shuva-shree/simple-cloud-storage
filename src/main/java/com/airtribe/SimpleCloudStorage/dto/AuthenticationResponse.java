package com.airtribe.SimpleCloudStorage.dto;

import jakarta.persistence.Entity;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private String token;


}
