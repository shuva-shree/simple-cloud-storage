package com.airtribe.SimpleCloudStorage.service;

import com.airtribe.SimpleCloudStorage.entity.File;
import com.airtribe.SimpleCloudStorage.enums.PermissionType;
import com.airtribe.SimpleCloudStorage.repository.FilePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FilePermissionService {

    @Autowired
    private FilePermissionRepository filePermissionRepository;

     public boolean canAccessFile(Integer userId, File file){
         if(file.getUserId() == userId)
             return true;

         if(file.isPublic())
             return true;
         int userid = (int)userId;

         return filePermissionRepository.existsByFileAndUserIdAndType(file, userid, PermissionType.READ);
     }

    public boolean canWriteToFile(int userId, File file) {
        if(file.getUserId() == userId)
            return true;

        if(file.isPublic())
            return true;
        int userid = (int)userId;

        return filePermissionRepository.existsByFileAndUserIdAndType(file, userid, PermissionType.WRITE);

    }
}
