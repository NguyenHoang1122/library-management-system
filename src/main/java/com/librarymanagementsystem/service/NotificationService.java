package com.librarymanagementsystem.service;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;

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

    void deleteReadNotifications(Long userId);

    long countUnreadNotifications(Long userId);

    // Thông báo cho thủ thư
    void notifyReviewBorrowRequest(BorrowRequest borrowRequest);

    void notifyReviewReturnRequest(ReturnRequest returnRequest);

    // Thông báo cho user
    void notifyBorrowApproved(BorrowTransaction transaction);

    void notifyBorrowRejected(BorrowRequest borrowRequest);

    void notifyBorrowReturned(BorrowTransaction transaction);

    void notifyReturnApproved(ReturnRequest returnRequest);

    void notifyReturnRejected(ReturnRequest returnRequest);

    void notifyBorrowRequestCancelled(BorrowRequest borrowRequest);

    void notifyBookDueSoon(BorrowTransaction transaction, int daysLeft);

    void notifyBookOverdue(BorrowTransaction transaction);

}
