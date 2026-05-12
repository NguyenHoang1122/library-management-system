package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.BorrowService;
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
@RequestMapping("/borrow")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class BorrowController {


    private final BorrowService borrowService;
    private final UserService userService;

    /**     * Gửi yêu cầu mượn sách     */
    @GetMapping("/request/{bookId}")
    public String showBorrowRequestForm(@PathVariable Long bookId, Model model) {
        model.addAttribute("bookId", bookId);
        return "borrow/request-form";
    }

    /**     * Xử lý gửi yêu cầu mượn     */
    @PostMapping("/request/{bookId}")
    public String submitBorrowRequest(@PathVariable Long bookId,
                                      @RequestParam(required = false) String note,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            borrowService.createBorrowRequest(user.getId(), bookId, note);
            redirectAttributes.addFlashAttribute("message", "Đã gửi yêu cầu mượn sách thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/borrow";
    }

    /**     * Xem danh sách yêu cầu mượn sách     */
    @GetMapping
    public String listBorrowRequests(Authentication authentication, Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<BorrowRequest> borrowRequests = borrowService.getUserBorrowRequests(user.getId());
        model.addAttribute("borrowRequests", borrowRequests);
        return "borrow/requests";
    }

    /**     * Xem lịch sử mượn sách     */
    @GetMapping("/history")
    public String showBorrowHistory(Authentication authentication, Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<BorrowTransaction> borrowHistory = borrowService.getUserBorrowHistory(user.getId());
        List<BorrowTransaction> activeBorrows = borrowService.getActiveBorrows(user.getId());

        model.addAttribute("borrowHistory", borrowHistory);
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("totalBorrows", borrowHistory.size());
        model.addAttribute("totalActive", activeBorrows.size());

        return "borrow/history";
    }

    /**     * Xem chi tiết giao dịch mượn     */
    @GetMapping("/{transactionId}")
    public String viewBorrowDetail(@PathVariable Long transactionId,
                                   Authentication authentication,
                                   Model model) {
        BorrowTransaction transaction = borrowService.getBorrowTransactionDetail(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        User currentUser = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Kiểm tra quyền
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền xem thông tin này");
        }

        // Tính tiền phạt
        long lateFine = borrowService.calculateLateFine(transactionId);
        boolean isOverdue = borrowService.isOverdue(transactionId);

        model.addAttribute("transaction", transaction);
        model.addAttribute("lateFine", lateFine);
        model.addAttribute("isOverdue", isOverdue);

        return "borrow/detail";
    }

    /**     * Hủy yêu cầu mượn     */
    @PostMapping("/{requestId}/cancel")
    public String cancelBorrowRequest(@PathVariable Long requestId,
                                      RedirectAttributes redirectAttributes) {
        try {
            borrowService.cancelBorrowRequest(requestId);
            redirectAttributes.addFlashAttribute("message", "Đã hủy yêu cầu mượn sách");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/borrow";
    }
}
