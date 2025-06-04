package com.airtribe.SimpleCloudStorage.entity;

import com.airtribe.SimpleCloudStorage.enums.FileStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@Builder
public class File {

    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int file_id;

    @Column(name = "user_id")
    private int userId;

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private Folder folder;
    private String fileName;
    private String s3_key;
    private long fileSize;
    private String fileType;
    private String filePath;
    private boolean isPublic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileStatus status = FileStatus.AVAILABLE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "file_tags",
            joinColumns = @JoinColumn(name = "file_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @CreationTimestamp
    private Date created_at;
    @UpdateTimestamp
    private Date updated_at;


    public File(){

    }

    public File(MultipartFile file, Users uploader, Folder destinationFolder) {

        this.userId = uploader.getUserId();
        this.folder = destinationFolder;
        this.fileName = file.getOriginalFilename();
        this.fileSize = file.getSize();
        this.fileType = file.getContentType();
        this.isPublic = false;
        this.created_at = new Date();
        this.updated_at = new Date();
    }
//
    public int getFile_id() {
        return file_id;
    }

    public void setFile_id(int file_id) {
        this.file_id = file_id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getS3_key() {
        return s3_key;
    }

    public void setS3_key(String s3_key) {
        this.s3_key = s3_key;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.updated_at = updated_at;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
}
