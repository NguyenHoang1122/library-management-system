package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;
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
        List<BorrowRequest> pendingRequests = borrowService.getAllPendingRequests();
        
        // Tìm kiếm
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            pendingRequests = pendingRequests.stream()
                .filter(r -> (r.getUser() != null && (
                                (r.getUser().getFullName() != null && r.getUser().getFullName().toLowerCase().contains(lowerQuery)) ||
                                (r.getUser().getUserName() != null && r.getUser().getUserName().toLowerCase().contains(lowerQuery)) ||
                                (r.getUser().getEmail() != null && r.getUser().getEmail().toLowerCase().contains(lowerQuery))
                             )) || 
                             (!r.getBorrowRequestItems().isEmpty() && 
                                r.getBorrowRequestItems().get(0).getBook() != null && 
                                r.getBorrowRequestItems().get(0).getBook().getTitle() != null && 
                                r.getBorrowRequestItems().get(0).getBook().getTitle().toLowerCase().contains(lowerQuery)
                             )
                )
                .collect(Collectors.toList());
        }

        // Phân trang
        int pageSize = 10;
        int totalItems = pendingRequests.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<BorrowRequest> pagedRequests = pendingRequests.subList(start, end);

        model.addAttribute("pendingRequests", pagedRequests);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
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
        List<ReturnRequest> pendingReturnRequests = borrowService.getAllPendingReturnRequests();
        
        // Tìm kiếm
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            pendingReturnRequests = pendingReturnRequests.stream()
                .filter(r -> r.getUser() != null && (
                                (r.getUser().getFullName() != null && r.getUser().getFullName().toLowerCase().contains(lowerQuery)) ||
                                (r.getUser().getUserName() != null && r.getUser().getUserName().toLowerCase().contains(lowerQuery)) ||
                                (r.getUser().getEmail() != null && r.getUser().getEmail().toLowerCase().contains(lowerQuery))
                             )
                )
                .collect(Collectors.toList());
        }

        // Phân trang
        int pageSize = 10;
        int totalItems = pendingReturnRequests.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<ReturnRequest> pagedRequests = pendingReturnRequests.subList(start, end);

        model.addAttribute("pendingReturnRequests", pagedRequests);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
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
        List<BorrowTransaction> activeBorrows = borrowService.getAllActiveBorrows();

        // Nhóm theo User và tính tổng số sách đang mượn
        Map<User, Integer> userBorrowCount = new HashMap<>();
        for (BorrowTransaction transaction : activeBorrows) {
            User user = transaction.getUser();
            if (user != null) {
                int itemCount = transaction.getItems().size();
                userBorrowCount.put(user, userBorrowCount.getOrDefault(user, 0) + itemCount);
            }
        }

        // Chuyển sang dạng Map sạch để truyền cho template tránh lỗi tuần tự hóa thực thể
        List<Map<String, Object>> memberBorrows = userBorrowCount.entrySet().stream()
                .map(entry -> {
                    User u = entry.getKey();
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", u.getId());
                    map.put("fullName", u.getFullName());
                    map.put("userName", u.getUserName());
                    map.put("email", u.getEmail());
                    map.put("borrowCount", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        // Tìm kiếm
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            memberBorrows = memberBorrows.stream()
                .filter(m -> {
                    String fullName = m.get("fullName") != null ? m.get("fullName").toString().toLowerCase() : "";
                    String userName = m.get("userName") != null ? m.get("userName").toString().toLowerCase() : "";
                    String email = m.get("email") != null ? m.get("email").toString().toLowerCase() : "";
                    return fullName.contains(lowerQuery) || userName.contains(lowerQuery) || email.contains(lowerQuery);
                })
                .collect(Collectors.toList());
        }

        // Sắp xếp
        memberBorrows.sort((m1, m2) -> Integer.compare((int) m2.get("borrowCount"), (int) m1.get("borrowCount")));

        // Phân trang
        int pageSize = 10;
        int totalItems = memberBorrows.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<Map<String, Object>> pagedMembers = memberBorrows.subList(start, end);

        model.addAttribute("memberBorrows", pagedMembers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("searchQuery", query);

        return "librarian/active-borrows";
    }

    // Xem chi tiết danh sách tất cả các cuốn truyện đang mượn của user
    @GetMapping("/active-borrows/user/{userId}")
    public String viewUserActiveBorrows(@PathVariable Long userId, Model model) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại"));

        List<BorrowTransaction> userActiveTransactions = borrowService.getAllActiveBorrows().stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .sorted((t1, t2) -> t2.getBorrowDate().compareTo(t1.getBorrowDate()))
                .collect(Collectors.toList());

        List<Map<String, Object>> borrowData = userActiveTransactions.stream().map(borrow -> {
            Map<String, Object> data = new HashMap<>();
            data.put("borrow", borrow);
            data.put("isOverdue", borrowService.isOverdue(borrow.getId()));
            data.put("lateFine", borrowService.calculateLateFine(borrow.getId()));
            data.put("itemCount", borrow.getItems().size());
            return data;
        }).collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("borrowDataMap", borrowData);
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
