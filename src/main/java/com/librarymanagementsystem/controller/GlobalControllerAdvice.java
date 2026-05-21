package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.book.Category;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.book.category.CategoryService;
import com.librarymanagementsystem.service.notification.NotificationService;
import com.librarymanagementsystem.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    private final UserService userService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;

    //thêm đối tượng user hiện tại vào Model
    @ModelAttribute("currentUser")
    public User addCurrentUserToModel(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userService.findByUserName(username).orElse(null);
        }
        return null;
    }

    //tải danh sách tất cả các thể loại truyện
    @ModelAttribute("categories")
    public List<Category> addCategoriesToModel() {
        try {
            return categoryService.getAllCategories();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    //lấy danh sách thông báo của người dùng đăng nhập hiện tại
    @ModelAttribute("notifications")
    public List<Notification> addNotificationsToModel(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUserName(authentication.getName()).orElse(null);
            if (user != null) {
                return notificationService.getUserNotifications(user.getId());
            }
        }
        return Collections.emptyList();
    }

    // số lượng thông báo chưa đọc của người dùng đăng nhập hiện tại
    @ModelAttribute("unreadCount")
    public long addUnreadCountToModel(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUserName(authentication.getName()).orElse(null);
            if (user != null) {
                return notificationService.countUnreadNotifications(user.getId());
            }
        }
        return 0;
    }

    // Kiểm tra xem danh mục phụ "Khác" có đang được chọn lọc hay không
    @ModelAttribute("isOtherCategoryActive")
    public boolean addIsOtherCategoryActive(HttpServletRequest request) {
        String[] categoryParams = request.getParameterValues("category");
        if (categoryParams == null || categoryParams.length == 0) {
            return false;
        }
        String param = categoryParams[0];

        List<Category> categories = categoryService.getAllCategories();
        if (categories == null || categories.size() <= 5) {
            return false;
        }

        List<String> otherIds = categories.subList(5, categories.size())
                .stream()
                .map(c -> String.valueOf(c.getId()))
                .collect(Collectors.toList());

        return otherIds.contains(param);
    }
}
