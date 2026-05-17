package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.dto.UserDTO;
import com.librarymanagementsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public String showUserDetail(Authentication authentication, Model model) {
        String userName = authentication.getName();
        User user = userService.findByUserName(userName).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        model.addAttribute("user", user);
        return "user/user-detail";
    }

    @GetMapping("/profile/edit")
    public String showProfileForm(Authentication authentication, Model model) {
        return "redirect:/user/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication, @ModelAttribute UserDTO userDTO,
                                RedirectAttributes redirectAttributes, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Dữ liệu không hợp lệ");
            return "redirect:/user/profile";
        }

        String userName = authentication.getName();
        User user = userService.findByUserName(userName).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        try {
            userService.updateProfile(user.getId(), userDTO);
            redirectAttributes.addFlashAttribute("message", "Cập nhật thông tin thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/profile";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String listActiveUsers(@RequestParam(defaultValue = "1") int page, Model model) {
        return searchActiveUsers(null, page, model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public String searchActiveUsers(@RequestParam(required = false) String query,
                                    @RequestParam(defaultValue = "1") int page,
                                    Model model) {
        List<User> users = userService.getAllActiveUsers();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            users = users.stream()
                .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerQuery)) ||
                             (u.getUserName() != null && u.getUserName().toLowerCase().contains(lowerQuery)) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
        }

        int pageSize = 10;
        int totalItems = users.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<User> pagedUsers = users.subList(start, end);

        model.addAttribute("users", pagedUsers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("searchQuery", query);
        model.addAttribute("isTrash", false);
        return "user/list-users";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/trash")
    public String listDeletedUsers(@RequestParam(defaultValue = "1") int page, Model model) {
        return searchDeletedUsers(null, page, model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/trash/search")
    public String searchDeletedUsers(@RequestParam(required = false) String query,
                                     @RequestParam(defaultValue = "1") int page,
                                     Model model) {
        List<User> deletedUsers = userService.getAllDeletedUsers();
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            deletedUsers = deletedUsers.stream()
                .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerQuery)) ||
                             (u.getUserName() != null && u.getUserName().toLowerCase().contains(lowerQuery)) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
        }

        int pageSize = 10;
        int totalItems = deletedUsers.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<User> pagedUsers = deletedUsers.subList(start, end);

        model.addAttribute("users", pagedUsers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("searchQuery", query);
        model.addAttribute("isTrash", true);
        return "user/trash";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/change-role")
    public String changeUserRole(@PathVariable Long id, @RequestParam String roleName,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.changeUserRole(id, roleName);
            redirectAttributes.addFlashAttribute("message", "Thay đổi role thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/soft-delete")
    public String softDeleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.softDeleteUser(id);
            redirectAttributes.addFlashAttribute("message", "Đã chuyển vào thùng rác");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/restore")
    public String restoreUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.restoreUser(id);
            redirectAttributes.addFlashAttribute("message", "Khôi phục người dùng thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/trash";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/permanently-delete")
    public String permanentlyDeleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.permanentlyDeleteUser(id);
            redirectAttributes.addFlashAttribute("message", "Xóa vĩnh viễn thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/trash";
    }
}
