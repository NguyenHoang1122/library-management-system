package com.librarymanagementsystem.service;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;

import java.util.List;
import java.util.Optional;

public interface NotificationService {

    List<Notification> getUserNotifications(Long userId);

    List<Notification> getUnreadNotifications(Long userId);

    Optional<Notification> getNotificationDetail(Long notificationId);

     void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId);

    void deleteAllNotifications(Long userId);

    long countUnreadNotifications(Long userId);

    void notifyReviewBorrowRequest(BorrowRequest borrowRequest);

    void notifyBorrowApproved(BorrowTransaction transaction);

    void notifyBorrowRejected(BorrowRequest borrowRequest);

    void notifyBorrowReturned(BorrowTransaction transaction);

    void notifyBorrowRequestCancelled(BorrowRequest borrowRequest);

    void notifyBookDueSoon(BorrowTransaction transaction, int daysLeft);

    void notifyBookOverdue(BorrowTransaction transaction);

}
