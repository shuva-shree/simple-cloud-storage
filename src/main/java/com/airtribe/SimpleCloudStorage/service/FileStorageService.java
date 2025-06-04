package com.airtribe.SimpleCloudStorage.service;

import com.airtribe.SimpleCloudStorage.dto.FileResponse;
import com.airtribe.SimpleCloudStorage.dto.FileVersionResponse;
import com.airtribe.SimpleCloudStorage.dto.S3UploadResult;
import com.airtribe.SimpleCloudStorage.entity.File;
import com.airtribe.SimpleCloudStorage.entity.Tag;
import com.airtribe.SimpleCloudStorage.entity.Folder;
import com.airtribe.SimpleCloudStorage.entity.Users;
import com.airtribe.SimpleCloudStorage.enums.FileStatus;
import com.airtribe.SimpleCloudStorage.exceptionHandler.FileStorageException;
import com.airtribe.SimpleCloudStorage.repository.FileRepository;
import com.airtribe.SimpleCloudStorage.repository.FolderRepository;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.services.s3.model.S3VersionSummary;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileStorageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 s3Client;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private FolderRepository folderRepository;
    @Autowired
    private FilePermissionService filePermissionService;


    public FileStorageService(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public File uploadFile(MultipartFile file, int userId, Integer folderId, boolean isPublic) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new FileStorageException("File name is null");
        }

        String extension = FilenameUtils.getExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString();
        String folderPath = (folderId != null) ? "folders/" + folderId : "root";
        String s3Key = String.format("users/%d/%s/%s.%s", userId, folderPath, uniqueFileName, extension);
        Folder folder = folderId != null ? folderRepository.findById(folderId).orElse(null) : null;

        try  {
            byte[] fileBytes = file.getBytes();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);


            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.addUserMetadata("uploaded-by", String.valueOf(userId));
            metadata.setCacheControl("max-age=31536000");

            PutObjectRequest request = new PutObjectRequest(bucket, s3Key, byteArrayInputStream, metadata);
            s3Client.putObject(request);

            String fileUrl = s3Client.getUrl(bucket, s3Key).toString();

            File fileEntity = File.builder()
                    .userId(userId)
                    .folder(folder)
                    .fileName(file.getOriginalFilename())
                    .s3_key(s3Key)
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .filePath(fileUrl)
                    .isPublic(isPublic)
                    .status(FileStatus.AVAILABLE)
                    .build();

            fileRepository.save(fileEntity);

            return fileEntity;
        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            throw new FileStorageException("Failed to upload file", e);
        }
    }

    public Resource loadFileAsResource(Integer fileId, String s3Key) {
        try {
            S3Object s3Object = s3Client.getObject(bucket, s3Key);
            byte[] bytes = s3Object.getObjectContent().readAllBytes();
            return new ByteArrayResource(bytes){
                @Override
                public String getFilename() {
                    return s3Key.substring(s3Key.lastIndexOf('/') + 1);
                }
            };
        } catch (IOException | AmazonS3Exception e) {
            log.error("Error loading file from S3", e);
            throw new FileStorageException(fileId, "download", e);
        }
    }

    public List<FileResponse> getAllFiles(int userId) {
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

    public List<FileResponse> searchFile(Integer userId, String query, String tag) {
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


        String key = file.getS3_key();

        ListVersionsRequest request = new ListVersionsRequest().withBucketName(bucket).withPrefix(key);
        VersionListing versionListing = s3Client.listVersions(request);

        List<FileVersionResponse> versions = new ArrayList<>();
        int counter = 1;
        for (S3VersionSummary summary : versionListing.getVersionSummaries()) {
            versions.add(new FileVersionResponse(counter++, summary.getLastModified().toInstant()));
        }

        return versions;
    }


    public void restoreFileVersion(Integer fileId, String versionId, Users user) throws FileNotFoundException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        if (!filePermissionService.canWriteToFile(user.getUserId(), file)) {
            throw new AccessDeniedException("You do not have permission to restore this file.");
        }


        String key = file.getS3_key();

        // Get the content of the specified version
        S3Object oldVersionObject = s3Client.getObject(new GetObjectRequest(bucket, key, versionId));
        InputStream contentStream = oldVersionObject.getObjectContent();

        // Upload it again (new version is created)
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(oldVersionObject.getObjectMetadata().getContentLength());
        s3Client.putObject(bucket, key, contentStream, metadata);
    }


    public File updateFile(Integer fileId, MultipartFile file, Integer userId, Boolean isPublic) throws FileNotFoundException {
        File fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (fileEntity.getUserId() != userId) {
            throw new AccessDeniedException("Unauthorized update");
        }

        deleteFromS3(fileEntity.getS3_key()); // remove old
        Integer folderId = fileEntity.getFolder().getFolder_id();


        File uploadedFile =uploadFile(file, userId, folderId, isPublic);
        String s3Key = uploadedFile.getS3_key();

        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setS3_key(s3Key);
        fileEntity.setFileType(file.getContentType());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setPublic(isPublic);

        return fileRepository.save(fileEntity);
    }

    public void deleteFile(Integer fileId, Integer userId) throws FileNotFoundException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (file.getUserId() != userId) {
            throw new AccessDeniedException("Unauthorized delete");
        }

        deleteFromS3(file.getS3_key());
        fileRepository.delete(file);
    }

    public void deleteFromS3(String s3Key) {
        try {
            s3Client.deleteObject(bucket, s3Key);
        } catch (AmazonServiceException e) {
            log.error("Error deleting file from S3", e);
            throw new RuntimeException("Failed to delete file from S3");
        }
    }


}
