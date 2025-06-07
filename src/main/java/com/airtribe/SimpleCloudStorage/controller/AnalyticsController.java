package com.airtribe.SimpleCloudStorage.controller;



import com.airtribe.SimpleCloudStorage.config.JwtService;
import com.airtribe.SimpleCloudStorage.dto.ErrorResponse;
import com.airtribe.SimpleCloudStorage.entity.Users;
import com.airtribe.SimpleCloudStorage.exceptionHandler.UnauthorizedException;
import com.airtribe.SimpleCloudStorage.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
        import lombok.extern.slf4j.Slf4j; // For logging

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final JwtService jwtService; // Assuming you need JWT validation for analytics APIs too

    // Helper method for token extraction (can be shared or put in a utility class)
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return authHeader.substring(7);
    }

    @GetMapping("/storage-usage")
    public ResponseEntity<?> getStorageUsage(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                             @AuthenticationPrincipal Users user) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.isTokenValid(token, user)) {
                throw new UnauthorizedException("Invalid or expired token");
            }
            Long usage = analyticsService.getStorageUsage(user.getUserId());
            return ResponseEntity.ok(Map.of("totalStorageBytes", usage != null ? usage : 0));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting storage usage for user {}: {}", user.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("SERVER_ERROR", "Failed to retrieve storage usage."));
        }
    }

    @GetMapping("/file-types")
    public ResponseEntity<?> getFileTypeDistribution(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                     @AuthenticationPrincipal Users user) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.isTokenValid(token, user)) {
                throw new UnauthorizedException("Invalid or expired token");
            }
            Map<String, Long> distribution = analyticsService.getFileTypeDistribution(user.getUserId());
            return ResponseEntity.ok(distribution);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting file type distribution for user {}: {}", user.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("SERVER_ERROR", "Failed to retrieve file type distribution."));
        }
    }

    @GetMapping("/access-frequency")
    public ResponseEntity<?> getAccessFrequency(@RequestParam(defaultValue = "DOWNLOAD") String eventType,
                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                @AuthenticationPrincipal Users user) {
        try {
            String token = extractToken(authHeader);
            if (!jwtService.isTokenValid(token, user)) {
                throw new UnauthorizedException("Invalid or expired token");
            }
            Map<String, Long> frequency = analyticsService.getFileAccessFrequency(user.getUserId(), eventType.toUpperCase());
            return ResponseEntity.ok(frequency);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting access frequency for user {}: {}", user.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("SERVER_ERROR", "Failed to retrieve access frequency."));
        }
    }
}
