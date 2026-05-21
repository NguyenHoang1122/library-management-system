package com.librarymanagementsystem.controller.librarian;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

    //danh sách các yêu cầu mượn truyện đang chờ duyệt
    @GetMapping("/borrows")
    public String listPendingBorrows(@RequestParam(defaultValue = "1") int page,
                                     Model model) {
        return searchPendingBorrows(null, page, model);
    }

    // Tìm kiếm các yêu cầu mượn truyện đang chờ phê duyệt
    @GetMapping("/borrows/search")
    public String searchPendingBorrows(@RequestParam(required = false) String query,
                                       @RequestParam(defaultValue = "1") int page,
                                       Model model) {
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<BorrowRequest> requestPage = borrowService.getPendingRequests(query, pageable);

        model.addAttribute("pendingRequests", requestPage.getContent());
        model.addAttribute("currentPage", requestPage.getNumber() + 1);
        model.addAttribute("totalPages", requestPage.getTotalPages() > 0 ? requestPage.getTotalPages() : 1);
        model.addAttribute("totalItems", requestPage.getTotalElements());
        model.addAttribute("searchQuery", query);

        return "librarian/borrows";
    }

    // Thủ thư duyệt yêu cầu mượn truyện
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

    // Thủ thư từ chối yêu cầu mượn truyện
    @PostMapping("/borrows/{requestId}/reject")
    public String rejectBorrow(@PathVariable Long requestId,
                               @RequestParam(required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            borrowService.rejectBorrowRequest(requestId, reason);
            redirectAttributes.addFlashAttribute("message", "Đã từ chối yêu cầu mượn truyện");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/borrows";
    }

    //các yêu cầu trả truyện đang chờ thủ thư xử lý
    @GetMapping("/returns")
    public String listPendingReturnRequests(@RequestParam(defaultValue = "1") int page,
                                            Model model) {
        return searchPendingReturnRequests(null, page, model);
    }

    // Tìm kiếm các yêu cầu trả sách trực tuyến đang chờ xử lý dựa theo thông tin của người trả sách
    @GetMapping("/returns/search")
    public String searchPendingReturnRequests(@RequestParam(required = false) String query,
                                              @RequestParam(defaultValue = "1") int page,
                                              Model model) {
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<ReturnRequest> requestPage = borrowService.getPendingReturnRequests(query, pageable);

        model.addAttribute("pendingReturnRequests", requestPage.getContent());
        model.addAttribute("currentPage", requestPage.getNumber() + 1);
        model.addAttribute("totalPages", requestPage.getTotalPages() > 0 ? requestPage.getTotalPages() : 1);
        model.addAttribute("totalItems", requestPage.getTotalElements());
        model.addAttribute("searchQuery", query);

        return "librarian/returns";
    }

    // danh sách user đang mượn truyện
    @GetMapping("/active-borrows")
    public String listAllActiveBorrows(@RequestParam(defaultValue = "1") int page,
                                       Model model) {
        return searchAllActiveBorrows(null, page, model);
    }

    // Thống kê và tìm kiếm danh sách độc giả đang mượn truyện, hiển thị tổng số cuốn đang giữ và hỗ trợ tìm theo từ khóa
    @GetMapping("/active-borrows/search")
    public String searchAllActiveBorrows(@RequestParam(required = false) String query,
                                         @RequestParam(defaultValue = "1") int page,
                                         Model model) {
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<Object[]> borrowersPage = borrowService.getActiveBorrowers(query, pageable);

        List<Map<String, Object>> pagedMembers = borrowersPage.getContent().stream()
                .map(arr -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", arr[0]);
                    map.put("fullName", arr[1]);
                    map.put("userName", arr[2]);
                    map.put("email", arr[3]);
                    map.put("borrowCount", ((Number) arr[4]).intValue());
                    return map;
                })
                .collect(Collectors.toList());

        model.addAttribute("memberBorrows", pagedMembers);
        model.addAttribute("currentPage", borrowersPage.getNumber() + 1);
        model.addAttribute("totalPages", borrowersPage.getTotalPages() > 0 ? borrowersPage.getTotalPages() : 1);
        model.addAttribute("totalItems", borrowersPage.getTotalElements());
        model.addAttribute("searchQuery", query);

        return "librarian/active-borrows";
    }

    // Xem chi tiết danh sách tất cả các cuốn truyện đang mượn của user
    @GetMapping("/active-borrows/user/{userId}")
    public String viewUserActiveBorrows(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "1") int page,
                                        Model model) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại"));

        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(
                page - 1, 10, Sort.by(Sort.Direction.DESC, "borrowDate")
        );

        Page<BorrowTransaction> userActiveTransactionsPage = borrowService.getActiveTransactionsPaged(userId, pageable);

        List<Map<String, Object>> borrowData = userActiveTransactionsPage.getContent().stream().map(borrow -> {
            Map<String, Object> data = new HashMap<>();
            data.put("borrow", borrow);
            data.put("isOverdue", borrowService.isOverdue(borrow.getId()));
            data.put("lateFine", borrowService.calculateLateFine(borrow.getId()));
            data.put("itemCount", borrow.getItems().size());
            return data;
        }).collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("borrowDataMap", borrowData);
        model.addAttribute("currentPage", userActiveTransactionsPage.getNumber() + 1);
        model.addAttribute("totalPages", userActiveTransactionsPage.getTotalPages() > 0 ? userActiveTransactionsPage.getTotalPages() : 1);
        model.addAttribute("totalItems", userActiveTransactionsPage.getTotalElements());

        return "librarian/user-active-borrows";
    }

    // Thủ thư phê duyệt yêu cầu trả truyện trực tuyến
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

    // Thủ thư xác nhận đã nhận đủ truyện từ người dùng và hoàn tất yêu cầu trả sách
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

    // Thủ thư từ chối yêu cầu trả truyện trực tuyến của user
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

    // Thủ thư thực hiện xác nhận trả truyện trực tiếp
    @PostMapping("/active-borrows/return/{transactionId}")
    public String returnBook(@PathVariable Long transactionId,
                             @RequestParam(required = false) Long userId,
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
        
        if (userId != null) {
            return "redirect:/librarian/active-borrows/user/" + userId;
        }
        return "redirect:/librarian/active-borrows";
    }
}
