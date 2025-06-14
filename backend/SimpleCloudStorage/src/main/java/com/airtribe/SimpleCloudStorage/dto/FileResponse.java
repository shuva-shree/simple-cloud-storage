package com.airtribe.SimpleCloudStorage.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor

public class FileResponse implements Serializable {
    @Getter
    @Setter
    int fileId;
    String filename;
    long filesize;
    boolean isPublic;
    String filetype;
    Date createdAt;
    private List<String> tags;


}
