package com.airtribe.SimpleCloudStorage.repository;

import com.airtribe.SimpleCloudStorage.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder,Integer> {
}
