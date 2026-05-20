package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.NotificationService;
import com.librarymanagementsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/my-notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'LIBRARIAN', 'ADMIN')")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String listNotifications(Authentication authentication, Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        List<Notification> unreadNotifications = notificationService.getUnreadNotifications(user.getId());
        long unreadCount = notificationService.countUnreadNotifications(user.getId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("totalNotifications", notifications.size());

        return "notification/my-notifications";
    }

    // Xem chi tiết thông báo
    @GetMapping("/{notificationId}")
    public String viewNotificationDetail(@PathVariable Long notificationId, Model model) {
        Notification notification = notificationService.getNotificationDetail(notificationId)
                .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));

        // Đánh dấu đã đọc
        if (!notification.isRead()) {
            notificationService.markAsRead(notificationId);
        }

        model.addAttribute("notification", notification);
        return "notification/detail";
    }

    // đánh dấu đã đọc
    @PostMapping("/{notificationId}/mark-read")
    public String markAsRead(@PathVariable Long notificationId,
                             RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(notificationId);
            redirectAttributes.addFlashAttribute("message", "Đã đánh dấu thông báo là đã đọc");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-notifications";
    }

    // AJAX API để đánh dấu đã đọc một thông báo một cách nhanh chóng mà không cần tải lại toàn bộ trang web
    @PostMapping("/{notificationId}/mark-read-ajax")
    @ResponseBody
    public java.util.Map<String, Object> markAsReadAjax(@PathVariable Long notificationId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            notificationService.markAsRead(notificationId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    // Đánh dấu tất cả các thông báo hiện có của người dùng hiện tại
    @PostMapping("/mark-all-read")
    public String markAllAsRead(Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            notificationService.markAllAsRead(user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã đánh dấu tất cả thông báo là đã đọc");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-notifications";
    }

    // Xóa vĩnh viễn một thông báo
    @PostMapping("/{notificationId}/delete")
    public String deleteNotification(@PathVariable Long notificationId,
                                     RedirectAttributes redirectAttributes) {
        try {
            notificationService.deleteNotification(notificationId);
            redirectAttributes.addFlashAttribute("message", "Đã xóa thông báo");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-notifications";
    }

    // Xóa toàn bộ các thông báo đã đọc của người dùng
    @PostMapping("/delete-read")
    public String deleteReadNotifications(Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            List<Notification> notifications = notificationService.getUserNotifications(user.getId());
            long readCount = notifications.stream().filter(Notification::isRead).count();
            if (readCount == 0) {
                redirectAttributes.addFlashAttribute("error", "Không có thông báo nào đã đọc để xóa!");
                return "redirect:/my-notifications";
            }
            notificationService.deleteReadNotifications(user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã xóa các thông báo đã đọc");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-notifications";
    }

    // AJAX API xóa toàn bộ các thông báo đã đọc giúp cập nhật nhanh giao diện mà không cần reload trang
    @PostMapping("/delete-read-ajax")
    @ResponseBody
    public java.util.Map<String, Object> deleteReadNotificationsAjax(Authentication authentication) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            List<Notification> notifications = notificationService.getUserNotifications(user.getId());
            long readCount = notifications.stream().filter(Notification::isRead).count();
            if (readCount == 0) {
                response.put("success", false);
                response.put("message", "Không có thông báo nào đã đọc để xóa!");
                return response;
            }
            notificationService.deleteReadNotifications(user.getId());
            response.put("success", true);
            response.put("message", "Đã xóa các thông báo đã đọc thành công");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    // Xóa sạch toàn bộ thông báo (bao gồm cả chưa đọc và đã đọc)
    @PostMapping("/delete-all")
    public String deleteAllNotifications(Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            notificationService.deleteAllNotifications(user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã xóa tất cả thông báo");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/my-notifications";
    }

    // AJAX API lấy nhanh số lượng đếm các thông báo chưa đọc để cập nhật theo thời gian thực trên giao diện client
    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(Authentication authentication) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return notificationService.countUnreadNotifications(user.getId());
    }
}
