package com.airtribe.SimpleCloudStorage.repository;

import com.airtribe.SimpleCloudStorage.entity.File;
import com.airtribe.SimpleCloudStorage.entity.FilePermission;
import com.airtribe.SimpleCloudStorage.enums.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilePermissionRepository extends JpaRepository<FilePermission,Long> {
    boolean existsByFileAndUserIdAndType(File file, int userId, PermissionType type);
}
