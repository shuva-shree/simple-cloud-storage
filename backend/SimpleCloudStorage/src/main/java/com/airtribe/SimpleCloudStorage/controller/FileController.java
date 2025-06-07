package com.airtribe.SimpleCloudStorage.controller;

import com.airtribe.SimpleCloudStorage.config.JwtService;
import com.airtribe.SimpleCloudStorage.dto.ErrorResponse;
import com.airtribe.SimpleCloudStorage.dto.FileResponse;
import com.airtribe.SimpleCloudStorage.dto.FileVersionResponse;
import com.airtribe.SimpleCloudStorage.dto.S3UploadResult;
import com.airtribe.SimpleCloudStorage.entity.File;
import com.airtribe.SimpleCloudStorage.entity.Folder;
import com.airtribe.SimpleCloudStorage.entity.Users;
import com.airtribe.SimpleCloudStorage.enums.FileStatus;
import com.airtribe.SimpleCloudStorage.exceptionHandler.FileUnavailableException;
import com.airtribe.SimpleCloudStorage.exceptionHandler.UnauthorizedException;
import com.airtribe.SimpleCloudStorage.repository.FileRepository;
import com.airtribe.SimpleCloudStorage.repository.FolderRepository;
import com.airtribe.SimpleCloudStorage.service.FilePermissionService;
import com.airtribe.SimpleCloudStorage.service.FileStorageService;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.airtribe.SimpleCloudStorage.entity.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final JwtService jwtService;
    private final FilePermissionService permissionService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestPart("file")  MultipartFile multipartFile,
                                        @RequestParam(required = false) Integer folderId,
                                        @RequestParam(defaultValue = "false") boolean isPublic,
                                        @AuthenticationPrincipal Users user) {
        try {
            if (multipartFile.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File cannot be empty", "FILE_EMPTY"));
            }


            File fileEntity = fileStorageService.uploadFile(multipartFile, user.getUserId(), folderId,isPublic);

            List<String> tagNames = Optional.ofNullable(fileEntity.getTags())
                            .orElse(Collections.emptySet())
                    .stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());

            FileResponse response = new FileResponse(
                    fileEntity.getFile_id(),
                    fileEntity.getFileName(),
                    fileEntity.getFileSize(),
                    fileEntity.isPublic(),
                    fileEntity.getFileType(),
                    fileEntity.getCreated_at(),
                    tagNames
            );

            return ResponseEntity.ok(response);

        } catch (AmazonS3Exception e) {
            log.error("S3 upload failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Storage service unavailable", "STORAGE_ERROR"));
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("File upload failed", "UPLOAD_FAILED"));
        }
    }
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer fileId,
                                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                 @AuthenticationPrincipal Users user)
            throws AccessDeniedException, FileNotFoundException {

        String token = extractToken(authHeader);

        if (!jwtService.isTokenValid(token, user)) {
            throw new AccessDeniedException("Invalid or expired token");
        }

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (file.getStatus() != FileStatus.AVAILABLE) {
            throw new FileUnavailableException("File is currently " + file.getStatus().getDisplayName());
        }

        if (!permissionService.canAccessFile(user.getUserId(), file)) {
            throw new AccessDeniedException("No permission to access this file");
        }

        Resource resource = fileStorageService.loadFileAsResource(fileId, file.getS3_key());

        // Extract filename and content type
        String filename = file.getFileName(); // Should include extension
        String fileType = file.getFileType(); // e.g., application/pdf, image/png
        log.info("fileType",fileType);
        log.info("fileName",filename);

        // Use Spring's ContentDisposition for better encoding
        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(filename)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-File-Id", String.valueOf(file.getFile_id()))
                .body(resource);
    }


    @GetMapping
    public ResponseEntity<List<FileResponse>> listUserFiles(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                            @AuthenticationPrincipal Users user) {
        String token = extractToken(authHeader);

        if (!jwtService.isTokenValid(token, user)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        List<FileResponse> response = fileStorageService.getAllFiles(user.getUserId());


        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<FileResponse>> searchFiles(
            @RequestParam("query") String query,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @AuthenticationPrincipal Users user
    ) {
        String token = extractToken(authHeader);
        if (!jwtService.isTokenValid(token, user)) {
            throw new UnauthorizedException("Invalid or expired token");
        }
        Integer userId = user.getUserId();
        List<FileResponse> response = fileStorageService.searchFile(userId,query,tag);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{fileId}/versions")
    public ResponseEntity<?> getFileVersions(
            @PathVariable Integer fileId,
            @AuthenticationPrincipal Users user) {

        try {
            // 1. Validate file exists and user has access
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new FileNotFoundException("File not found"));

            if (!permissionService.canAccessFile(user.getUserId(), file)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("ACCESS_DENIED", "You don't have access to this file"));
            }

            // 2. Get versions from storage service
            List<FileVersionResponse> versions = fileStorageService.listFileVersions(fileId);

            // 3. Format response data consistently
            versions.forEach(version -> {
                // Ensure date is in milliseconds since epoch (JavaScript-friendly format)
                log.error("Last Modified Date: {}",version.getCreatedAt());
                log.error("Last Modified Date: {}",version.getVersion());
                log.error("Last Modified Date: {}",version.getSize());
                if (version.getCreatedAt() != null) {
                    if (version.getCreatedAt() instanceof Instant) {
                        version.setCreatedAt((Instant) version.getCreatedAt());
                    } else if (version.getCreatedAt() instanceof Temporal) {
                        version.setCreatedAt(Instant.from((Temporal) version.getCreatedAt()));
                    }
                }


                version.setSize(version.getSize() != null ? version.getSize() : 0L);
            });

            return ResponseEntity.ok(versions);

        } catch (FileNotFoundException e) {
            log.error("File not found: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "File not found with ID: " + fileId));
        } catch (Exception e) {
            log.error("Error retrieving versions for file: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Failed to retrieve versions. Please try again."));
        }
    }
    @PostMapping("/{fileId}/versions/{versionId}/restore")
    public ResponseEntity<?> restoreVersion(@PathVariable Integer fileId,
                                            @PathVariable String versionId,
                                            @AuthenticationPrincipal Users user) {
        try {
            fileStorageService.restoreFileVersion(fileId, versionId, user);
            return ResponseEntity.ok(Map.of("message", "Version restored successfully"));
        }  catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (AmazonS3Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("S3_ERROR", e.getErrorMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("RESTORE_FAILED", "Something went wrong while restoring the version"));
        }
    }

    @PutMapping(value = "/{fileId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateFile(@PathVariable Integer fileId,
                                        @RequestPart("file") MultipartFile multipartFile,
                                        @RequestParam(required = false) Boolean isPublic,
                                        @AuthenticationPrincipal Users user) {
        try {
            if (multipartFile.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File cannot be empty", "FILE_EMPTY"));
            }
            boolean isPublicValue = (isPublic != null) ? isPublic : false;

            File updatedFile = fileStorageService.updateFile(fileId, multipartFile, user.getUserId(), isPublicValue);

            List<String> tagNames = Optional.ofNullable(updatedFile.getTags())
                    .orElse(Collections.emptySet())
                    .stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());

            FileResponse response = new FileResponse(
                    updatedFile.getFile_id(),
                    updatedFile.getFileName(),
                    updatedFile.getFileSize(),
                    updatedFile.isPublic(),
                    updatedFile.getFileType(),
                    updatedFile.getCreated_at(),
                    tagNames
            );

            return ResponseEntity.ok(response);

        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage(), "FILE_NOT_FOUND"));
        } catch (AmazonS3Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Storage service unavailable", "STORAGE_ERROR"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("File update failed", "UPDATE_FAILED"));
        }
    }
    @DeleteMapping("delete/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Integer fileId,
                                        @AuthenticationPrincipal Users user) {
        try {
            fileStorageService.deleteFile(fileId, user.getUserId());

            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));

        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage(), "FILE_NOT_FOUND"));
        } catch (AmazonS3Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Storage service unavailable", "STORAGE_ERROR"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("File deletion failed", "DELETE_FAILED"));
        }
    }








    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}
