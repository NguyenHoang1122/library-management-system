package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.dto.UserDTO;
import com.librarymanagementsystem.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import com.librarymanagementsystem.service.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;


import java.util.Map;

import java.util.List;
import java.util.stream.Collectors;

import com.librarymanagementsystem.service.user.AdminWalletService;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final VNPayService vnPayService;
    private final AdminWalletService adminWalletService;

    // Xử lý nạp/rút tiền ví ảo hệ thống dành cho Admin
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/profile/adjust-wallet")
    public String adjustAdminWallet(@RequestParam Double amount,
                                    @RequestParam String action,
                                    @RequestParam String reason,
                                    RedirectAttributes redirectAttributes) {
        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "Số tiền không hợp lệ");
            return "redirect:/user/profile";
        }
        
        String cleanReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : 
                (action.equals("deposit") ? "Nạp bổ sung quỹ hệ thống" : "Rút quỹ chi tiêu hệ thống");

        try {
            double finalAmount = action.equals("deposit") ? amount : -amount;
            adminWalletService.logTransaction(finalAmount, "MANUAL_ADJUST", cleanReason, null);
            
            String msg = action.equals("deposit") ? 
                    "Đã nạp quỹ hệ thống thành công " + String.format("%,.0f", amount) + " đ." :
                    "Đã rút quỹ hệ thống thành công " + String.format("%,.0f", amount) + " đ.";
            redirectAttributes.addFlashAttribute("message", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/user/profile";
    }

    // Hiển thị thông tin hồ sơ chi tiết (Profile) của người dùng đang đăng nhập hiện tại
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

    // Xử lý cập nhật thông tin cá nhân
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

    //danh sách user
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
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<User> userPage = userService.getActiveUsers(query, pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", userPage.getNumber() + 1);
        model.addAttribute("totalPages", userPage.getTotalPages() > 0 ? userPage.getTotalPages() : 1);
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("searchQuery", query);
        model.addAttribute("isTrash", false);
        return "user/list-users";
    }

    // Các user bị xóa mềm
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/trash")
    public String listDeletedUsers(@RequestParam(defaultValue = "1") int page, Model model) {
        return searchDeletedUsers(null, page, model);
    }

    // Tìm kiếm trong danh sách user bị xóa mềm
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/trash/search")
    public String searchDeletedUsers(@RequestParam(required = false) String query,
                                     @RequestParam(defaultValue = "1") int page,
                                     Model model) {
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<User> userPage = userService.getDeletedUsers(query, pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", userPage.getNumber() + 1);
        model.addAttribute("totalPages", userPage.getTotalPages() > 0 ? userPage.getTotalPages() : 1);
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("searchQuery", query);
        model.addAttribute("isTrash", true);
        return "user/trash";
    }

    //Đổi vai trò
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

    // xóa mềm
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

    // Khôi phục user
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

    // Xóa vĩnh viễn user
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

    @PostMapping("/deposit")
    @ResponseBody
    public ResponseEntity<?> deposit(@RequestParam Double amount, HttpServletRequest request, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        if (amount == null || amount <= 10000) return ResponseEntity.badRequest().body(Map.of("message", "Số tiền không hợp lệ (phải > 10.000)"));
        
        try {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String orderInfo = "Nap tien vao tai khoan " + user.getId();
            String paymentUrl = vnPayService.createOrder(amount.intValue(), orderInfo, baseUrl);
            
            return ResponseEntity.ok(Map.of("success", true, "redirectUrl", paymentUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        boolean isValidSignature = vnPayService.verifySignature(request);
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_OrderInfo = request.getParameter("vnp_OrderInfo");
        String vnp_Amount = request.getParameter("vnp_Amount"); // Amount is multiplied by 100 in VNPAY
        
        model.addAttribute("isSuccess", false);
        model.addAttribute("message", "Giao dịch không thành công hoặc bị hủy.");
        
        if (isValidSignature && "00".equals(vnp_ResponseCode)) {
            try {
                // Parse userId từ OrderInfo (VD: "Nap tien vao tai khoan 1")
                String[] parts = vnp_OrderInfo.split(" ");
                Long userId = Long.parseLong(parts[parts.length - 1]);
                Double amount = Double.parseDouble(vnp_Amount) / 100.0;
                
                userService.deposit(userId, amount);
                model.addAttribute("isSuccess", true);
                model.addAttribute("message", "Nạp tiền thành công! Đã cộng " + String.format("%,.0f", amount) + "đ vào tài khoản.");
            } catch (Exception e) {
                model.addAttribute("message", "Có lỗi xảy ra khi cộng tiền: " + e.getMessage());
            }
        }
        return "user/vnpay-result";
    }

    // Rút tiền
    @PostMapping("/withdraw")
    @ResponseBody
    public ResponseEntity<?> withdraw(@RequestParam Double amount, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        if (amount == null || amount <= 0) return ResponseEntity.badRequest().body(Map.of("message", "Số tiền không hợp lệ"));
        
        try {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            Double newBalance = userService.withdraw(user.getId(), amount);
            return ResponseEntity.ok(Map.of("success", true, "newBalance", newBalance));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
