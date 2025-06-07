package com.airtribe.SimpleCloudStorage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
public class UploadedFileInfo {
    @Getter
    @Setter
    private String s3Key;
    private String filePath;
    private long fileSize;
    private String fileType;
    private String originalFilename;


}
