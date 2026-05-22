package com.librarymanagementsystem.service.notification.impl;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import com.librarymanagementsystem.repository.notification.NotificationRepository;
import com.librarymanagementsystem.service.notification.NotificationService;
import com.librarymanagementsystem.service.user.UserService;
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
    private final UserService userService;

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    //thông báo chưa đọc
    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadNotificationsSinceDate(userId, LocalDateTime.now().minusDays(30));
    }

    // chi tiết thông báo
    @Override
    public Optional<Notification> getNotificationDetail(Long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    // check đã đọc
    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    // check all đã đọc
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

    // Xóa thông báo đã đọc
    @Override
    public void deleteReadNotifications(Long userId) {
        notificationRepository.deleteReadNotifications(userId);
    }

    // count thông báo chưa đọc
    @Override
    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    // Tạo và lưu thông báo gửi tới thủ thư yêu cầu mượn truyện
    @Override
    public void notifyReviewBorrowRequest(BorrowRequest borrowRequest) {
        List<User> librarians = userService.getAllActiveUsers().stream()
                .filter(user -> user.getRole().getRoleName() == RoleStatus.ROLE_LIBRARIAN)
                .toList();
        for (User librarian : librarians) {
            Notification notification = new Notification();
            notification.setUser(librarian);
            notification.setTitle("Yêu cầu mượn truyện mới");
            notification.setContent("Có một yêu cầu mượn truyện mới từ user " + borrowRequest.getUser().getFullName() +
                    " vào ngày " + borrowRequest.getRequestDate().toLocalDate() +
                    ". Vui lòng kiểm tra và duyệt yêu cầu.");
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    //Thông báo Phê duyệt truyện cho user
    @Override
    public void notifyBorrowApproved(BorrowTransaction transaction) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("Yêu cầu mượn truyện được duyệt");
        notification.setContent("Yêu cầu mượn truyện của bạn đã được thủ thư duyệt. " +
                 "Vui lòng đến thư viện để nhận truyện. " +
                 "Hạn trả: " + transaction.getDueDate().toLocalDate());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Thông báo từ chối mượn cho user
    @Override
    public void notifyBorrowRejected(BorrowRequest borrowRequest) {
        Notification notification = new Notification();
        notification.setUser(borrowRequest.getUser());
        notification.setTitle("Yêu cầu mượn truyện bị từ chối");
        String content = "Yêu cầu mượn truyện của bạn đã bị từ chối.";
        if (borrowRequest.getRejectionReason() != null && !borrowRequest.getRejectionReason().trim().isEmpty()) {
            content += " Lý do: " + borrowRequest.getRejectionReason();
        } else {
            content += " Vui lòng liên hệ thủ thư để biết thêm chi tiết.";
        }
        notification.setContent(content);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Thông báo hoản trả truyện thành công
    @Override
    public void notifyBorrowReturned(BorrowTransaction transaction) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("Truyện được xác nhận trả");
        notification.setContent("Giao dịch mượn truyện #" + transaction.getId() + " của bạn đã được xác nhận trả.");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Gửi thông báo xác nhận yêu cầu mượn truyện của người dùng đã bị hủy
    @Override
    public void notifyBorrowRequestCancelled(BorrowRequest borrowRequest) {
        Notification notification = new Notification();
        notification.setUser(borrowRequest.getUser());
        notification.setTitle("Yêu cầu mượn truyện bị hủy");
        notification.setContent("Yêu cầu mượn truyện của bạn đã bị hủy.");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Gửi thông báo nhắc nhở người dùng khi truyện mượn sắp đến hạn trả
    @Override
    public void notifyBookDueSoon(BorrowTransaction transaction, int daysLeft) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("Nhắc nhở: truyện sắp hết hạn");
        notification.setContent("Bạn còn " + daysLeft + " ngày để trả truyện. " +
                 "Vui lòng trả truyện trước ngày " + transaction.getDueDate().toLocalDate());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Gửi thông báo cảnh báo người dùng khi truyện mượn của họ đã quá hạn trả
    @Override
    public void notifyBookOverdue(BorrowTransaction transaction) {
        Notification notification = new Notification();
        notification.setUser(transaction.getUser());
        notification.setTitle("⚠️ Cảnh báo: Truyện quá hạn");
        notification.setContent("Truyện của bạn đã quá hạn trả từ ngày " + transaction.getDueDate().toLocalDate() +
                 ". Vui lòng trả truyện tại thư viện ngay lập tức");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // thông báo tới thủ thư khi user gửi yêu cầu trả truyện
    @Override
    public void notifyReviewReturnRequest(ReturnRequest returnRequest) {
        List<User> librarians = userService.getAllActiveUsers().stream()
                .filter(user -> user.getRole().getRoleName() == RoleStatus.ROLE_LIBRARIAN)
                .toList();
        for (User librarian : librarians) {
            Notification notification = new Notification();
            notification.setUser(librarian);
            notification.setTitle("Yêu cầu trả truyện mới");
            notification.setContent("Có một yêu cầu trả truyện mới từ user " + returnRequest.getUser().getFullName() +
                     " vào ngày " + returnRequest.getRequestDate().toLocalDate() +
                     ". Thời gian trả dự kiến: " + returnRequest.getReturnDateTime().toLocalDate() +
                     ". Vui lòng kiểm tra và duyệt yêu cầu.");
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    // Gửi thông báo cho người dùng khi yêu cầu trả truyện của họ đã được thủ thư phê duyệt thành công
    @Override
    public void notifyReturnApproved(ReturnRequest returnRequest) {
        Notification notification = new Notification();
        notification.setUser(returnRequest.getUser());
        notification.setTitle("Yêu cầu trả truyện đã được duyệt");
        notification.setContent("Yêu cầu trả truyện của bạn cho giao dịch #" + returnRequest.getBorrowTransaction().getId() +
                 " đã được thủ thư duyệt thành công.");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Gửi thông báo cho người dùng khi yêu cầu trả truyện của họ bị thủ thư từ chối cùng lý do từ chối
    @Override
    public void notifyReturnRejected(ReturnRequest returnRequest) {
        Notification notification = new Notification();
        notification.setUser(returnRequest.getUser());
        notification.setTitle("Yêu cầu trả truyện bị từ chối");
        notification.setContent("Yêu cầu trả truyện của bạn cho giao dịch #" + returnRequest.getBorrowTransaction().getId() +
                 " đã bị từ chối. Lý do: " + returnRequest.getRejectionReason());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Gửi thông báo chung
    @Override
    public void sendNotification(User user, String title, String content, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        // Lưu link vào nội dung hoặc tuỳ theo thiết kế database (hiện tại nối thêm vào nội dung)
        if (link != null && !link.isEmpty()) {
            content += " (Link: " + link + ")";
        }
        notification.setContent(content);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
