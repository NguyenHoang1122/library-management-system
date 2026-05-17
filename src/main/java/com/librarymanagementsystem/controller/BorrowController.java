package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.BookService;
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
    private final BookService bookService;

    /**     * Gửi yêu cầu mượn sách     */
    @GetMapping("/request/{bookId}")
    public String showBorrowRequestForm(@PathVariable Long bookId, Model model) {
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        model.addAttribute("book", book);
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

    @PostMapping("/quick-request/{bookId}")
    @ResponseBody
    public java.util.Map<String, Object> quickBorrowRequest(@PathVariable Long bookId,
                                                           @RequestParam(required = false) String note,
                                                           Authentication authentication) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            borrowService.createBorrowRequest(user.getId(), bookId, note);
            response.put("success", true);
            response.put("message", "Đã gửi yêu cầu mượn truyện thành công! Số lượng trong kho đã được cập nhật.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
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

        List<BorrowHistoryDTO> borrowHistory = borrowService.getUserBorrowHistory(user.getId());
        List<BorrowHistoryDTO> activeBorrows = borrowService.getActiveBorrows(user.getId());

        model.addAttribute("borrowHistory", borrowHistory);
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("totalBorrows", borrowHistory.size());
        model.addAttribute("totalActive", activeBorrows.size());

        return "borrow/history";
    }

    @GetMapping("/active")
    public String showActiveBorrows(Authentication authentication, Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        List<BorrowHistoryDTO> activeBorrows = borrowService.getActiveBorrows(user.getId());
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("activePage", "activeborrows");
        return "borrow/active";
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

    @PostMapping("/{transactionId}/return")
    public String requestReturn(@PathVariable Long transactionId,
                                @RequestParam String returnDateTime,
                                @RequestParam(required = false) String note,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            // Chuyển đổi String sang LocalDateTime
            java.time.LocalDateTime returnTime;
            try {
                returnTime = java.time.LocalDateTime.parse(returnDateTime);
            } catch (java.time.format.DateTimeParseException e) {
                throw new RuntimeException("Định dạng ngày giờ không hợp lệ. Vui lòng thử lại.");
            }
            
            borrowService.createReturnRequest(user.getId(), transactionId, returnTime, note);
            redirectAttributes.addFlashAttribute("message", "Đã gửi yêu cầu trả truyện thành công. Vui lòng chờ thủ thư xác nhận.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/borrow/active";
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
