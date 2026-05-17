package com.librarymanagementsystem.repository;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy thông báo theo user có sắp xếp
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);


    // Lấy thông báo chưa đọc theo ngày
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.createdAt >= :date ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsSinceDate(@Param("userId") Long userId, @Param("date") LocalDateTime date);

    // Đánh dấu thông báo là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);

    // Đánh dấu tất cả thông báo của user là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.isRead = true")
    void deleteReadNotifications(@Param("userId") Long userId);
}
