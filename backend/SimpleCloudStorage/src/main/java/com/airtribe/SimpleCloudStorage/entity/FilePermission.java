package com.airtribe.SimpleCloudStorage.entity;

import com.airtribe.SimpleCloudStorage.enums.PermissionType;
import jakarta.persistence.*;

@Entity
public class FilePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long permissionId;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private File file;
    @Column(name = "user_id")
    private int userId;

    @Enumerated(EnumType.STRING)
    private PermissionType type;

    public boolean hasReadAccess() {
        return type == PermissionType.READ;
    }

    public boolean hasWriteAccess() {
        return type == PermissionType.WRITE;
    }
    public boolean hasDeleteAccess() {
        return type == PermissionType.DELETE;
    }
}
