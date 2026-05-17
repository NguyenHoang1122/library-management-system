package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import com.librarymanagementsystem.service.BorrowService;
import com.librarymanagementsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/librarian")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LIBRARIAN')")
public class LibrarianController {

    private final BorrowService borrowService;
    private final UserService userService;

    @GetMapping("/borrows")
    public String listPendingBorrows(Model model) {
        List<BorrowRequest> pendingRequests = borrowService.getAllPendingRequests();
        model.addAttribute("pendingRequests", pendingRequests);
        return "librarian/borrows";
    }

    @PostMapping("/borrows/{requestId}/approve")
    public String approveBorrow(@PathVariable Long requestId,
                                @RequestParam(defaultValue = "14") Integer borrowDays,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User librarian = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Librarian not found"));
            borrowService.approveBorrowRequest(requestId, librarian.getId(), borrowDays);
            redirectAttributes.addFlashAttribute("message", "Đã duyệt yêu cầu mượn truyện");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/borrows";
    }

    @PostMapping("/borrows/{requestId}/reject")
    public String rejectBorrow(@PathVariable Long requestId,
                               RedirectAttributes redirectAttributes) {
        try {
            borrowService.rejectBorrowRequest(requestId);
            redirectAttributes.addFlashAttribute("message", "Đã từ chối yêu cầu mượn truyện");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/borrows";
    }

    @GetMapping("/returns")
    public String listPendingReturnRequests(Model model) {
        model.addAttribute("pendingReturnRequests", borrowService.getAllPendingReturnRequests());
        return "librarian/returns";
    }

    @GetMapping("/active-borrows")
    public String listAllActiveBorrows(Model model) {
        List<BorrowTransaction> activeBorrows = borrowService.getAllActiveBorrows();

        // Chuẩn bị dữ liệu cho template (giống như cũ nhưng tách ra trang riêng)
        List<Map<String, Object>> borrowData = activeBorrows.stream().map(borrow -> {
            Map<String, Object> data = new HashMap<>();
            data.put("borrow", borrow);
            data.put("isOverdue", borrowService.isOverdue(borrow.getId()));
            data.put("lateFine", borrowService.calculateLateFine(borrow.getId()));
            data.put("itemCount", borrow.getItems().size());
            return data;
        }).collect(Collectors.toList());

        model.addAttribute("borrowDataMap", borrowData);
        return "librarian/active-borrows";
    }

    @PostMapping("/returns/approve/{requestId}")
    public String approveReturn(@PathVariable Long requestId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User librarian = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Librarian not found"));
            borrowService.approveReturnRequest(requestId, librarian.getId());
            redirectAttributes.addFlashAttribute("message", "Đã duyệt yêu cầu trả truyện thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/returns";
    }

    @PostMapping("/returns/complete/{requestId}")
    public String completeReturn(@PathVariable Long requestId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            User librarian = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Librarian not found"));
            borrowService.completeReturnRequest(requestId, librarian.getId());
            redirectAttributes.addFlashAttribute("message", "Đã xác nhận trả truyện thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/returns";
    }

    @PostMapping("/returns/reject/{requestId}")
    public String rejectReturn(@PathVariable Long requestId,
                               @RequestParam String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            borrowService.rejectReturnRequest(requestId, reason);
            redirectAttributes.addFlashAttribute("message", "Đã từ chối yêu cầu trả truyện");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/returns";
    }

    @PostMapping("/returns/{transactionId}")
    public String returnBook(@PathVariable Long transactionId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User librarian = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Librarian not found"));

            // Kiểm tra quá hạn trước khi trả
            BorrowTransaction transaction = borrowService.getBorrowTransactionDetail(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            boolean isOverdue = borrowService.isOverdue(transactionId);
            long fine = borrowService.calculateLateFine(transactionId);

            borrowService.returnBorrowItems(transactionId, librarian.getId());

            String message = "Đã xử lý trả truyện thành công";
            if (isOverdue) {
                message += String.format(" (Quá hạn, phạt: %,d VND)", fine);
            }
            redirectAttributes.addFlashAttribute("message", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/active-borrows";
    }

    @GetMapping("/members")
    public String listMembers(Model model) {
        List<User> members = userService.getAllActiveUsers().stream()
                .filter(user -> user.getRole().getRoleName() == RoleStatus.ROLE_USER)
                .toList();
        model.addAttribute("members", members);
        return "librarian/members";
    }
}
