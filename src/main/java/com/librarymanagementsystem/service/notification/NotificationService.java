package com.librarymanagementsystem.service.notification;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.user.User;

import java.util.List;
import java.util.Optional;

public interface NotificationService {

    List<Notification> getUserNotifications(Long userId);

    //các thông báo chưa đọc của người dùng
    List<Notification> getUnreadNotifications(Long userId);

    // chi tiết của một thông báo
    Optional<Notification> getNotificationDetail(Long notificationId);

    // Đánh dấu thông báo cụ thể đã đọc
    void markAsRead(Long notificationId);

    // Đánh dấu tất cả thông báo đã đọc
    void markAllAsRead(Long userId);

    // Xóa một thông báo cụ thể
    void deleteNotification(Long notificationId);

    // Xóa toàn bộ thông báo
    void deleteAllNotifications(Long userId);

    // Xóa tất cả các thông báo đã đọc
    void deleteReadNotifications(Long userId);

    // Đếm tổng số thông báo chưa đọc
    long countUnreadNotifications(Long userId);

    // Thông báo cho thủ thư

    // thông báo yêu cầu xét duyệt một yêu cầu mượn
    void notifyReviewBorrowRequest(BorrowRequest borrowRequest);

    // thông báo yêu cầu xét duyệt một yêu cầu trả truyện
    void notifyReviewReturnRequest(ReturnRequest returnRequest);

    // Thông báo cho user
    //thông báo được duyệt thành công
    void notifyBorrowApproved(BorrowTransaction transaction);

    // thông báo bị từ chối
    void notifyBorrowRejected(BorrowRequest borrowRequest);

    // thông báo được hoàn trả thành công
    void notifyBorrowReturned(BorrowTransaction transaction);

    // thông báo trả sách được phê duyệt
    void notifyReturnApproved(ReturnRequest returnRequest);

    // thông báo trả sách bị từ chối
    void notifyReturnRejected(ReturnRequest returnRequest);

    // Gửi thông báo cho thủ thư/hệ thống khi người dùng hủy bỏ yêu cầu mượn sách của họ
    void notifyBorrowRequestCancelled(BorrowRequest borrowRequest);

    // thông báo nhắc nhở sách sắp hết hạn
    void notifyBookDueSoon(BorrowTransaction transaction, int daysLeft);

    // thông báo cảnh báo sách đã quá hạn
    void notifyBookOverdue(BorrowTransaction transaction);

    // Gửi thông báo chung
    void sendNotification(User user, String title, String content, String link);

    // Thông báo sách sắp hết
    void notifyLowStock(Book book);

}
