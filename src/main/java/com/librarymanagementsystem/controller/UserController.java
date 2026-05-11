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
        String userName = authentication.getName();
        User user = userService.findByUserName(userName).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        UserDTO userDTO = new UserDTO();
        userDTO.setFullName(user.getFullName());
        userDTO.setUserName(user.getUserName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setAddress(user.getAddress());
        model.addAttribute("userDTO", userDTO);
        model.addAttribute("user", user);
        return "user/edit-profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication, @ModelAttribute UserDTO userDTO,
                                RedirectAttributes redirectAttributes, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Dữ liệu không hợp lệ");
            return "redirect:/user/profile/edit";
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
    public String listActiveUsers(Model model) {
        List<User> users = userService.getAllActiveUsers();
        model.addAttribute("users", users);
        model.addAttribute("isTrash", false);
        return "user/list-users";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/trash")
    public String listDeletedUsers(Model model) {
        List<User> deletedUsers = userService.getAllDeletedUsers();
        model.addAttribute("users", deletedUsers);
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
