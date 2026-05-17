package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.book.Category;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.CategoryService;
import com.librarymanagementsystem.service.NotificationService;
import com.librarymanagementsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    private final UserService userService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;
    @ModelAttribute("currentUser")
    public User addCurrentUserToModel(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userService.findByUserName(username).orElse(null);
        }
        return null;
    }

    @ModelAttribute("categories")
    public List<Category> addCategoriesToModel() {
        try {
            return categoryService.getAllCategories();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

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
}
