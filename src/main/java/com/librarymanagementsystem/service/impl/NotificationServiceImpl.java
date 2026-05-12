package com.librarymanagementsystem.service.impl;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.repository.NotificationRepository;
import com.librarymanagementsystem.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadNotificationsSinceDate(userId, LocalDateTime.now().minusDays(30));
    }

    @Override
    public Optional<Notification> getNotificationDetail(Long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public void deleteAllNotifications(Long userId) {
        // Lấy tất cả thông báo của user rồi xóa
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifications);
    }

    @Override
    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    public void notifyReviewBorrowRequest(BorrowRequest borrowRequest) {
        Notification notification = new Notification();
        notification.setUser(borrowRequest.getUser());
        notification.setTitle("Yêu cầu mượn sách mới");
        notification.setContent("Bạn có một yêu cầu mượn sách mới từ " + borrowRequest.getRequestDate().toLocalDate() +
                ". Vui lòng kiểm tra và duyệt yêu cầu.");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyBorrowApproved(BorrowTransaction transaction) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("Yêu cầu mượn sách được duyệt");
        notification.setContent("Yêu cầu mượn sách của bạn đã được thủ thư duyệt. " +
                "Vui lòng đến thư viện để nhận sách. " +
                "Hạn trả: " + transaction.getDueDate().toLocalDate());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyBorrowRejected(BorrowRequest borrowRequest) {
        Notification notification = new Notification();
        notification.setUser(borrowRequest.getUser());
        notification.setTitle("Yêu cầu mượn sách bị từ chối");
        notification.setContent("Yêu cầu mượn sách của bạn đã bị từ chối. " +
                "Vui lòng liên hệ thủ thư để biết thêm chi tiết.");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyBorrowReturned(BorrowTransaction transaction) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("Sách được xác nhận trả");
        notification.setContent("Giao dịch mượn sách #" + transaction.getId() + " của bạn đã được xác nhận trả.");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyBorrowRequestCancelled(BorrowRequest borrowRequest) {
        Notification notification = new Notification();
        notification.setUser(borrowRequest.getUser());
        notification.setTitle("Yêu cầu mượn sách bị hủy");
        notification.setContent("Yêu cầu mượn sách của bạn đã bị hủy.");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyBookDueSoon(BorrowTransaction transaction, int daysLeft) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("Nhắc nhở: Sách sắp hết hạn");
        notification.setContent("Bạn còn " + daysLeft + " ngày để trả sách. " +
                "Vui lòng trả sách trước ngày " + transaction.getDueDate().toLocalDate());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyBookOverdue(BorrowTransaction transaction) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("⚠️ Cảnh báo: Sách quá hạn");
        notification.setContent("Sách của bạn đã quá hạn trả từ ngày " + transaction.getDueDate().toLocalDate() +
                ". Vui lòng trả sách tại thư viện ngay lập tức");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
