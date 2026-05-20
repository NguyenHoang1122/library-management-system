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

    // Tìm kiếm user theo tên tài khoản
    Optional<User> findByUserName(String userName);

    // Tìm kiếm user theo địa chỉ email
    Optional<User> findByEmail(String email);

    //Check user có hay chưa
    boolean existsByUserName(String userName);

    //Check mail có hay chưa
    boolean existsByEmail(String email);

    // User đang hoạt động
    @Query("SELECT u FROM User u WHERE u.deleteAt IS NULL")
    List<User> findAllActiveUsers();

    // User xóa mềm
    @Query("SELECT u FROM User u WHERE u.deleteAt IS NOT NULL")
    List<User> findAllDeletedUsers();

    //người dùng đã bị xóa mềm trước time cụ thể.
    @Query("SELECT u FROM User u WHERE u.deleteAt IS NOT NULL AND u.deleteAt < :cutoffDate")
    List<User> findUsersToPermanentlyDelete(@Param("cutoffDate") LocalDateTime cutoffDate);

    // xóa người dùng quá hạn.
    @Modifying
    @Query("DELETE FROM User u WHERE u.deleteAt IS NOT NULL AND u.deleteAt < :cutoffDate")
    void permanentlyDeleteOldUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    // xóa cứng
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id = :id")
    void hardDeleteById(@Param("id") Long id);
}
