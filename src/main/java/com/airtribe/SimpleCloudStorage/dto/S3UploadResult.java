package com.airtribe.SimpleCloudStorage.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class S3UploadResult {
    private String s3Key;
    private String fileUrl;
}
