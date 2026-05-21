package com.librarymanagementsystem.controller.borrow;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.book.BookService;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.service.user.UserService;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/borrow")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class BorrowController {


    private final BorrowService borrowService;
    private final UserService userService;
    private final BookService bookService;

    @GetMapping("/request/{bookId}")
    public String showBorrowRequestForm(@PathVariable Long bookId, Model model) {
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        model.addAttribute("book", book);
        model.addAttribute("bookId", bookId);
        return "borrow/request-form";
    }

    @PostMapping("/request/{bookId}")
    public String submitBorrowRequest(@PathVariable Long bookId,
                                      @RequestParam(required = false) String note,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            borrowService.createBorrowRequest(user.getId(), bookId, note);
            redirectAttributes.addFlashAttribute("message", "Đã gửi yêu cầu mượn truyện thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/borrow";
    }

    // Xử lý nhanh yêu cầu mượn truyên
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

    // Hiển thị danh sách các yêu cầu mượn truyện chờ duyệt hoặc đã xử lý
    @GetMapping
    public String listBorrowRequests(@RequestParam(defaultValue = "1") int page,
                                     Authentication authentication,
                                     Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<BorrowRequest> requestPage = borrowService.getUserBorrowRequests(user.getId(), pageable);

        model.addAttribute("borrowRequests", requestPage.getContent());
        model.addAttribute("currentPage", requestPage.getNumber() + 1);
        model.addAttribute("totalPages", requestPage.getTotalPages() > 0 ? requestPage.getTotalPages() : 1);
        model.addAttribute("totalItems", requestPage.getTotalElements());
        return "borrow/requests";
    }

    //Lịch sử mượn trả truyện
    @GetMapping("/history")
    public String showBorrowHistory(@RequestParam(defaultValue = "1") int page,
                                    Authentication authentication,
                                    Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<BorrowHistoryDTO> historyPage = borrowService.getUserBorrowHistory(user.getId(), pageable);
        
        long totalActive = borrowService.getActiveBorrows(user.getId(), PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("borrowHistory", historyPage.getContent());
        model.addAttribute("currentPage", historyPage.getNumber() + 1);
        model.addAttribute("totalPages", historyPage.getTotalPages() > 0 ? historyPage.getTotalPages() : 1);
        model.addAttribute("totalBorrows", historyPage.getTotalElements());
        model.addAttribute("totalActive", totalActive);

        return "borrow/history";
    }

    //danh sách các truyện người dùng đang mượn
    @GetMapping("/active")
    public String showActiveBorrows(@RequestParam(defaultValue = "1") int page,
                                    Authentication authentication,
                                    Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<BorrowHistoryDTO> activePage = borrowService.getActiveBorrows(user.getId(), pageable);

        model.addAttribute("activeBorrows", activePage.getContent());
        model.addAttribute("currentPage", activePage.getNumber() + 1);
        model.addAttribute("totalPages", activePage.getTotalPages() > 0 ? activePage.getTotalPages() : 1);
        model.addAttribute("totalItems", activePage.getTotalElements());
        model.addAttribute("activePage", "activeborrows");
        return "borrow/active";
    }

    // chi tiết của một giao dịch mượn trả
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

    //gửi yêu cầu trả truyện
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

    //người dùng tự hủy yêu cầu mượn
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
