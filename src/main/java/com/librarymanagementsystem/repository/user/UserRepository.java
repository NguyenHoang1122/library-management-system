package com.librarymanagementsystem.repository.user;

import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Tìm kiếm list user theo Role
    List<User> findByRoleRoleName(RoleStatus roleName);

    //Check user có hay chưa
    boolean existsByUserName(String userName);

    //Check mail có hay chưa
    boolean existsByEmail(String email);

    // User đang hoạt động
    @Query("SELECT u FROM User u WHERE u.deleteAt IS NULL")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.deleteAt IS NULL AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> findActiveUsers(@Param("query") String query, Pageable pageable);

    // User xóa mềm
    @Query("SELECT u FROM User u WHERE u.deleteAt IS NOT NULL")
    List<User> findAllDeletedUsers();

    @Query("SELECT u FROM User u WHERE u.deleteAt IS NOT NULL AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> findDeletedUsers(@Param("query") String query, Pageable pageable);

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

    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();

    @Query(value = "SELECT DATE(create_date) as date, COUNT(id) as count FROM users GROUP BY DATE(create_date) ORDER BY date DESC LIMIT 30", nativeQuery = true)
    List<Object[]> countDailyNewUsers();

    @Query(value = "SELECT DATE_FORMAT(create_date, '%Y') as date, COUNT(id) as count FROM users GROUP BY DATE_FORMAT(create_date, '%Y') ORDER BY date DESC LIMIT 5", nativeQuery = true)
    List<Object[]> countYearlyNewUsers();

    @Query(value = "SELECT DATE_FORMAT(create_date, '%Y-%m') as date, COUNT(id) as count FROM users GROUP BY DATE_FORMAT(create_date, '%Y-%m') ORDER BY date DESC LIMIT 12", nativeQuery = true)
    List<Object[]> countMonthlyNewUsers();
}
