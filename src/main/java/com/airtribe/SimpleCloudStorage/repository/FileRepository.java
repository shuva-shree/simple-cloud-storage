package com.airtribe.SimpleCloudStorage.repository;


import com.airtribe.SimpleCloudStorage.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File,Integer> {
    Optional<File> findById(int file_id);




    @Query("SELECT f FROM File f " +
            "WHERE f.userId = :userId OR " +
            "f.isPublic = true OR " +
            "EXISTS (SELECT 1 FROM FilePermission fp WHERE fp.file = f AND fp.userId = :userId)")
    List<File> findAccessibleFiles(@Param("userId") int userId);

    @Query("SELECT f FROM File f WHERE f.userId = :userId AND " +
            "(LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<File> searchByName(@Param("userId") int userId, @Param("query") String query);

    @Query("SELECT f FROM File f JOIN f.tags t WHERE f.userId = :userId AND " +
            "(LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.name) = LOWER(:tag))")
    List<File> searchByNameOrTag(@Param("userId") int userId,
                                 @Param("query") String query,
                                 @Param("tag") String tag);


    Optional<File> findByFileHash(String fileHash);
}
