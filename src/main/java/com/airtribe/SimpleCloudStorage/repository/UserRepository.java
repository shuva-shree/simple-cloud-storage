package com.airtribe.SimpleCloudStorage.repository;

import com.airtribe.SimpleCloudStorage.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users,Integer> {
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Users> findByUsername(String username);
}
