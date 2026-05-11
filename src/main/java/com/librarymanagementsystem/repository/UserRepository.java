package com.librarymanagementsystem.repository;

import com.librarymanagementsystem.model.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    // Thêm các method mới cho admin
    @Query("SELECT u FROM User u WHERE u.deleteAt IS NULL")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.deleteAt IS NOT NULL")
    List<User> findAllDeletedUsers();

    @Query("SELECT u FROM User u WHERE u.deleteAt IS NOT NULL AND u.deleteAt < :cutoffDate")
    List<User> findUsersToPermanentlyDelete(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM User u WHERE u.deleteAt IS NOT NULL AND u.deleteAt < :cutoffDate")
    void permanentlyDeleteOldUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id = :id")
    void hardDeleteById(@Param("id") Long id);
}
