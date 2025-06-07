package com.airtribe.SimpleCloudStorage.service;

import com.airtribe.SimpleCloudStorage.dto.FileResponse;
import com.airtribe.SimpleCloudStorage.dto.FileVersionResponse;
import com.airtribe.SimpleCloudStorage.entity.File;
import com.airtribe.SimpleCloudStorage.entity.Tag;
import com.airtribe.SimpleCloudStorage.entity.Folder;
import com.airtribe.SimpleCloudStorage.entity.Users;
import com.airtribe.SimpleCloudStorage.enums.FileStatus;
import com.airtribe.SimpleCloudStorage.exceptionHandler.FileStorageException;
import com.airtribe.SimpleCloudStorage.repository.AnalyticsEventRepository; // New Import
import com.airtribe.SimpleCloudStorage.repository.FileRepository;
import com.airtribe.SimpleCloudStorage.repository.FolderRepository;
import com.airtribe.SimpleCloudStorage.entity.AnalyticsEvent; // New Import

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest; // For hashing
import java.security.NoSuchAlgorithmException; // For hashing
import java.time.Instant; // Use Instant for timestamps
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional // Ensure transactions for database operations, including analytics events
public class FileStorageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 s3Client;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FilePermissionService filePermissionService;
    private final AnalyticsEventRepository analyticsEventRepository; // New dependency

    @Autowired // Use constructor injection for all dependencies
    public FileStorageService(AmazonS3 s3Client, FileRepository fileRepository,
                              FolderRepository folderRepository, FilePermissionService filePermissionService,
                              AnalyticsEventRepository analyticsEventRepository) {
        this.s3Client = s3Client;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.filePermissionService = filePermissionService;
        this.analyticsEventRepository = analyticsEventRepository; // Initialize new dependency
    }


    private String calculateSha256Hash(MultipartFile multipartFile) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = multipartFile.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashedBytes = digest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes); // URL-safe Base64
    }

    @CacheEvict(value = {"files", "searchResults"}, allEntries = true)
    @Transactional // Ensure atomicity for file and analytics operations
    public File uploadFile(MultipartFile file, int userId, Integer folderId, boolean isPublic) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new FileStorageException("File name is null");
        }

        String extension = FilenameUtils.getExtension(originalFilename);
        String fileType = file.getContentType();
        long fileSize = file.getSize();

        //Calculate file hash for deduplication
        String fileHash;
        try {
            fileHash = calculateSha256Hash(file);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Error calculating file hash: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to calculate file hash", e);
        }

        // 2. Determine S3 key and handle deduplication
        String newS3Key; // This will be the S3 key for the *new* file entity
        String sourceS3KeyForCopy = null; // S3 key of an existing identical object to copy from
        Optional<File> existingFileWithSameHashOpt = fileRepository.findByFileHash(fileHash);

        if (existingFileWithSameHashOpt.isPresent()) {
            // Deduplication scenario: content already exists in S3
            sourceS3KeyForCopy = existingFileWithSameHashOpt.get().getS3_key();
            log.info("Deduplication: Content with hash {} already exists. Source S3 Key for copy: {}", fileHash, sourceS3KeyForCopy);
        }

        // Generate a new S3 key for the new logical file entry
        // This is important for S3 versioning to work independently per 'File' record in your DB.
        // Format: users/{userId}/[folders/{folderId}/]{timestamp_uuid.extension}
        String folderPath = (folderId != null) ? "folders/" + folderId : "root";
        newS3Key = String.format("users/%d/%s/%s_%s.%s", userId, folderPath, Instant.now().toEpochMilli(), UUID.randomUUID().toString(), extension);

        try {
            if (sourceS3KeyForCopy != null) {
                // If content is duplicated, copy the S3 object instead of uploading
                // This saves bandwidth and processing time.
                s3Client.copyObject(bucket, sourceS3KeyForCopy, bucket, newS3Key);
                log.info("Deduplication: Copied S3 object from '{}' to '{}'.", sourceS3KeyForCopy, newS3Key);
            } else {
                // No duplicate content, proceed with normal S3 upload
                byte[] fileBytes = file.getBytes();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(fileSize);
                metadata.setContentType(fileType);
                metadata.addUserMetadata("uploaded-by", String.valueOf(userId));
                metadata.setCacheControl("max-age=31536000"); // Example cache control

                PutObjectRequest request = new PutObjectRequest(bucket, newS3Key, byteArrayInputStream, metadata);
                s3Client.putObject(request);
                log.info("Uploaded new file to S3 with key: '{}'.", newS3Key);
            }

            // Get folder entity if folderId is provided
            Folder folder = folderId != null ? folderRepository.findById(folderId).orElse(null) : null;

            // 3. Create and Save File Entity
            File fileEntity = File.builder()
                    .userId(userId)
                    .folder(folder)
                    .fileName(originalFilename)
                    .s3_key(newS3Key) // Store the newly generated S3 key
                    .fileSize(fileSize)
                    .fileType(fileType)
                    .filePath(s3Client.getUrl(bucket, newS3Key).toString()) // Generate URL for the new key
                    .isPublic(isPublic)
                    .status(FileStatus.AVAILABLE)
                    .fileHash(fileHash) // Store the calculated hash
                    .build();

            File savedFile = fileRepository.save(fileEntity);

            // 4. Log Analytics Event for Upload
            AnalyticsEvent uploadEvent = new AnalyticsEvent();
            uploadEvent.setUserId(userId);
            uploadEvent.setEventType("UPLOAD");
            uploadEvent.setTimestamp(Instant.now());
            uploadEvent.setFileSize(fileSize);
            uploadEvent.setFileType(fileType);
            uploadEvent.setFileId(savedFile.getFile_id());
            analyticsEventRepository.save(uploadEvent);
            log.info("Analytics: Logged UPLOAD event for file ID: {}", savedFile.getFile_id());

            return savedFile;
        } catch (IOException e) {
            log.error("Error processing file during upload/deduplication: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to upload file due to IO error", e);
        } catch (AmazonServiceException e) {
            log.error("AWS S3 error during upload/copy: {}", e.getMessage(), e);
            throw new FileStorageException("S3 service error during file operation", e);
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw new FileStorageException("File upload failed due to unexpected error", e);
        }
    }


    @Transactional // Log download event
    public Resource loadFileAsResource(Integer fileId, String s3Key) {
        try {
            S3Object s3Object = s3Client.getObject(bucket, s3Key);
            byte[] bytes = s3Object.getObjectContent().readAllBytes();

            // Log Analytics Event for Download
            File file = fileRepository.findById(fileId).orElse(null);
            if (file != null) {
                AnalyticsEvent downloadEvent = new AnalyticsEvent();
                downloadEvent.setUserId(file.getUserId()); // Assuming ownerId is the user who downloaded
                downloadEvent.setEventType("DOWNLOAD");
                downloadEvent.setTimestamp(Instant.now());
                downloadEvent.setFileSize(file.getFileSize());
                downloadEvent.setFileType(file.getFileType());
                downloadEvent.setFileId(fileId);
                analyticsEventRepository.save(downloadEvent);
                log.info("Analytics: Logged DOWNLOAD event for file ID: {}", fileId);
            }

            return new ByteArrayResource(bytes){
                @Override
                public String getFilename() {
                    return s3Key.substring(s3Key.lastIndexOf('/') + 1);
                }
            };
        } catch (IOException | AmazonS3Exception e) {
            log.error("Error loading file from S3 (fileId: {}): {}", fileId, e.getMessage(), e);
            throw new FileStorageException(fileId, "download", e);
        }
    }

    @Cacheable(value = "files", key = "#userId")
    public List<FileResponse> getAllFiles(int userId) {
        log.info("Fetching all files for user {} from database (potentially cached).", userId);
        List<File> accessibleFiles = fileRepository.findAccessibleFiles(userId);
        return accessibleFiles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private FileResponse convertToDto(File file) {
        List<String> tagNames = Optional.ofNullable(file.getTags()).orElse(Collections.emptySet()).stream()
                .map(Tag::getName)
                .collect(Collectors.toList());

        return new FileResponse(
                file.getFile_id(),
                file.getFileName(),
                file.getFileSize(),
                file.isPublic(),
                file.getFileType(),
                file.getCreated_at(),
                tagNames
        );
    }

    @Cacheable(value = "searchResults", key = "#userId + '-' + #query + '-' + #tag")
    public List<FileResponse> searchFile(Integer userId, String query, String tag) {
        log.info("Searching files for user {} with query '{}' and tag '{}' (potentially cached).", userId, query, tag);
        List<File> matchedFiles;

        if (tag != null && !tag.isBlank()) {
            matchedFiles = fileRepository.searchByNameOrTag(userId, query, tag);
        } else {
            matchedFiles = fileRepository.searchByName(userId, query);
        }

        return matchedFiles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    public List<FileVersionResponse> listFileVersions(Integer fileId) throws FileNotFoundException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        String key = file.getS3_key(); // The S3 key for the logical file

        ListVersionsRequest request = new ListVersionsRequest().withBucketName(bucket).withPrefix(key);
        VersionListing versionListing = s3Client.listVersions(request);

        List<FileVersionResponse> versions = new ArrayList<>();
        for (S3VersionSummary summary : versionListing.getVersionSummaries()) {
            log.debug("Found S3 version: ID={}, LastModified={}, Size={}", summary.getVersionId(), summary.getLastModified(), summary.getSize());
            versions.add(new FileVersionResponse(summary.getVersionId(), summary.getLastModified().toInstant(), summary.getSize()));
        }

        // Log analytics event for viewing versions (optional, can be done in controller too)
        AnalyticsEvent viewVersionsEvent = new AnalyticsEvent();
        viewVersionsEvent.setUserId(file.getUserId()); // Assuming ownerId is the user
        viewVersionsEvent.setEventType("VIEW_VERSIONS");
        viewVersionsEvent.setTimestamp(Instant.now());
        viewVersionsEvent.setFileId(fileId);
        analyticsEventRepository.save(viewVersionsEvent);
        log.info("Analytics: Logged VIEW_VERSIONS event for file ID: {}", fileId);

        return versions;
    }


    @Transactional // Log restore event
    public void restoreFileVersion(Integer fileId, String versionId, Users user) throws FileNotFoundException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        if (!filePermissionService.canWriteToFile(user.getUserId(), file)) {
            throw new AccessDeniedException("You do not have permission to restore this file.");
        }

        String key = file.getS3_key();

        try {
            // Get the content of the specified version
            S3Object oldVersionObject = s3Client.getObject(new GetObjectRequest(bucket, key, versionId));
            InputStream contentStream = oldVersionObject.getObjectContent();

            // Upload it again (this creates a *new* version in S3, making the restored content current)
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(oldVersionObject.getObjectMetadata().getContentLength());
            metadata.setContentType(oldVersionObject.getObjectMetadata().getContentType()); // Keep original content type
            s3Client.putObject(bucket, key, contentStream, metadata);

            // Update the file entity's last_modified timestamp
            file.setUpdated_at(Date.from(Instant.now())); // Assuming 'updated_at' is now 'last_modified' of type Instant
            fileRepository.save(file);

            // Log Analytics Event for Restore
            AnalyticsEvent restoreEvent = new AnalyticsEvent();
            restoreEvent.setUserId(user.getUserId());
            restoreEvent.setEventType("RESTORE_VERSION");
            restoreEvent.setTimestamp(Instant.now());
            restoreEvent.setFileId(fileId);
            // Optionally, include original file size/type of the restored version
            restoreEvent.setFileSize(oldVersionObject.getObjectMetadata().getContentLength());
            restoreEvent.setFileType(oldVersionObject.getObjectMetadata().getContentType());
            analyticsEventRepository.save(restoreEvent);
            log.info("Analytics: Logged RESTORE_VERSION event for file ID: {}", fileId);

        } catch (AmazonServiceException e) {
            log.error("AWS S3 error during version restore (fileId: {}): {}", fileId, e.getMessage(), e);
            throw new FileStorageException("S3 service error during version restore", e);
        }  catch (Exception e) {
            log.error("Unexpected error during version restore (fileId: {}): {}", fileId, e.getMessage(), e);
            throw new FileStorageException("Version restore failed due to unexpected error", e);
        }
    }


    @CacheEvict(value = {"files", "searchResults"}, allEntries = true)
    @Transactional // Log update event
    public File updateFile(Integer fileId, MultipartFile file, Integer userId, Boolean isPublic) throws FileNotFoundException {
        try {
            File fileEntity = fileRepository.findById(fileId)
                    .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

            // Ensure 'ownerId' is used for permission check
            if (fileEntity.getUserId() != userId){ // Assuming 'ownerId' is the field for file owner
                throw new AccessDeniedException("User not authorized to update this file");
            }

            log.info("Starting file update for fileId: {}", fileId);
            log.info("Overwriting existing file in S3: {}", fileEntity.getS3_key());

            // Overwrite file at existing s3_key (S3 versioning will handle new version)
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.addUserMetadata("updated-by", String.valueOf(userId)); // Optional metadata

            s3Client.putObject(bucket, fileEntity.getS3_key(), file.getInputStream(), metadata);

            // Update existing entity metadata
            fileEntity.setFileName(file.getOriginalFilename());
            fileEntity.setFileType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setPublic(isPublic);
            fileEntity.setUpdated_at(Date.from(Instant.now())); // Update last_modified

            File updatedFile = fileRepository.save(fileEntity);

            // Log Analytics Event for Update
            AnalyticsEvent updateEvent = new AnalyticsEvent();
            updateEvent.setUserId(userId);
            updateEvent.setEventType("UPDATE");
            updateEvent.setTimestamp(Instant.now());
            updateEvent.setFileSize(file.getSize());
            updateEvent.setFileType(file.getContentType());
            updateEvent.setFileId(fileId);
            analyticsEventRepository.save(updateEvent);
            log.info("Analytics: Logged UPDATE event for file ID: {}", fileId);

            return updatedFile;

        } catch (FileNotFoundException | AccessDeniedException e) {
            throw e;
        } catch (IOException e) {
            log.error("IO error while updating file (fileId={}): {}", fileId, e.getMessage(), e);
            throw new FileStorageException("Failed to process file data", e);
        } catch (AmazonServiceException e) {
            log.error("AWS S3 error during update (fileId={}): {}", fileId, e.getMessage(), e);
            throw new FileStorageException("S3 service error during file update", e);
        } catch (Exception e) {
            log.error("Unexpected error during file update (fileId={}): {}", fileId, e.getMessage(), e);
            throw new FileStorageException("File update failed due to unexpected error", e);
        }
    }


    @CacheEvict(value = {"files", "searchResults"}, allEntries = true)
    @Transactional // Log delete event
    public void deleteFile(Integer fileId, Integer userId) throws FileNotFoundException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        // Ensure 'ownerId' is used for permission check
        if (file.getUserId() != userId) { // Assuming 'ownerId' is the field for file owner
            throw new AccessDeniedException("Unauthorized delete");
        }

        // 1. Delete from S3 (this deletes all versions if S3 versioning is enabled)
        deleteFromS3(file.getS3_key());
        log.info("Deleted S3 object with key: '{}' for file ID: {}", file.getS3_key(), fileId);

        // 2. Delete from database
        fileRepository.delete(file);
        log.info("Deleted file entity from database for file ID: {}", fileId);

        // 3. Log Analytics Event for Delete
        AnalyticsEvent deleteEvent = new AnalyticsEvent();
        deleteEvent.setUserId(userId);
        deleteEvent.setEventType("DELETE");
        deleteEvent.setTimestamp(Instant.now());
        deleteEvent.setFileId(fileId);
        // Optionally, include file size/type of the deleted file
        deleteEvent.setFileSize(file.getFileSize());
        deleteEvent.setFileType(file.getFileType());
        analyticsEventRepository.save(deleteEvent);
        log.info("Analytics: Logged DELETE event for file ID: {}", fileId);
    }


    public void deleteFromS3(String s3Key) {
        try {
            // This sends a delete marker if versioning is on, or permanently deletes if not.
            // To delete all versions (including old ones), you'd need to iterate through versions.
            s3Client.deleteObject(bucket, s3Key);
        } catch (AmazonServiceException e) {
            log.error("Error deleting file from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
}