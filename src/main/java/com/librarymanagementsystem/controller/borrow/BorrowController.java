package com.librarymanagementsystem.controller.borrow;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.*;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;
import com.librarymanagementsystem.model.borrow.dto.CombinedHistoryDTO;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.book.BookService;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
    public Map<String, Object> quickBorrowRequest(@PathVariable Long bookId,
                                                           @RequestParam(required = false) String note,
                                                           Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
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

    //Lịch sử mượn trả truyện (Combined)
    @GetMapping("/history")
    public String showBorrowHistory(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(required = false) Long bookId,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String statusFilter,
                                    @RequestParam(required = false) String sortOption,
                                    Authentication authentication,
                                    Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10);
        
        Page<CombinedHistoryDTO> historyPage = borrowService.getCombinedBorrowHistory(user.getId(), keyword, bookId, statusFilter, sortOption, pageable);
        
        long totalActive = borrowService.getActiveBorrows(user.getId(), PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("borrowHistory", historyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages() > 0 ? historyPage.getTotalPages() : 1);
        model.addAttribute("totalBorrows", historyPage.getTotalElements());
        model.addAttribute("totalActive", totalActive);
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("sortOption", sortOption);

        return "borrow/history";
    }

    @GetMapping("/request/detail/{requestId}")
    public String viewRequestDetail(@PathVariable Long requestId, Authentication authentication, Model model) {
        User currentUser = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Map<String, Object> detailData = borrowService.getBorrowRequestDetailForUser(requestId, currentUser.getId());
        model.addAllAttributes(detailData);

        return "borrow/request-detail";
    }

    //danh sách các truyện người dùng đang mượn
    @GetMapping("/active")
    public String showActiveBorrows(@RequestParam(defaultValue = "1") int page,
                                    Authentication authentication,
                                    Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (page < 1) page = 1;
        
        List<Map<String, Object>> activeBooks = borrowService.getActiveBooksGrouped(user.getId());

        int pageSize = 10;
        int totalItems = activeBooks.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<Map<String, Object>> paginatedBooks = new ArrayList<>();
        if (start < totalItems) {
            paginatedBooks = activeBooks.subList(start, end);
        }

        model.addAttribute("activeBorrows", paginatedBooks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
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

        List<Map<String, Object>> groupedItems = borrowService.getGroupedItemsForTransaction(transactionId);
        model.addAttribute("groupedItems", groupedItems);

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
            LocalDateTime returnTime;
            try {
                returnTime = LocalDateTime.parse(returnDateTime);
            } catch (DateTimeParseException e) {
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
    @PostMapping("/request/cancel/{requestId}")
    public String cancelBorrowRequest(@PathVariable Long requestId,
                                      RedirectAttributes redirectAttributes) {
        try {
            borrowService.cancelBorrowRequest(requestId);
            redirectAttributes.addFlashAttribute("message", "Đã hủy đơn mượn sách thành công. Tiền đã được hoàn lại vào ví của bạn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/borrow/history";
    }

    // Gửi yêu cầu trả truyện từ trang chi tiết đơn
    @PostMapping("/request/return/{requestId}")
    public String requestPartialReturn(@PathVariable Long requestId,
                                       @RequestParam String returnMethod,
                                       @RequestParam(required = false) String returnAddress,
                                       @RequestParam java.util.Map<String, String> allParams,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            // Parse danh sách truyện và số lượng trả
            Map<Long, Integer> returnItems = new HashMap<>();
            for (java.util.Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("return_qty_")) {
                    Long bookId = Long.parseLong(entry.getKey().replace("return_qty_", ""));
                    Integer qty = Integer.parseInt(entry.getValue());
                    if (qty > 0) {
                        returnItems.put(bookId, qty);
                    }
                }
            }

            if (returnItems.isEmpty()) {
                throw new RuntimeException("Vui lòng chọn số lượng truyện cần trả.");
            }

            borrowService.createPartialReturnRequest(user.getId(), requestId, returnItems, returnMethod, returnAddress);
            redirectAttributes.addFlashAttribute("message", "Đã gửi yêu cầu trả truyện thành công. Vui lòng chờ xác nhận.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/borrow/request/detail/" + requestId;
    }

    // Gia hạn từ trang chi tiết đơn
    @PostMapping("/request/extend/{requestId}")
    public String extendBorrowRequest(@PathVariable Long requestId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            borrowService.extendBorrowRequest(user.getId(), requestId);
            redirectAttributes.addFlashAttribute("message", "Gia hạn thành công. Thời gian trả đã được cộng thêm 7 ngày.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gia hạn thất bại: " + e.getMessage());
        }
        return "redirect:/borrow/request/detail/" + requestId;
    }

    // Gia hạn mượn sách
    @PostMapping("/extend/{transactionId}")
    public String extendBorrowTransaction(@PathVariable Long transactionId,
                                          RedirectAttributes redirectAttributes) {
        try {
            borrowService.extendBorrowTransaction(transactionId);
            redirectAttributes.addFlashAttribute("message", "Gia hạn thành công. Thời gian trả đã được cộng thêm 7 ngày.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gia hạn thất bại: " + e.getMessage());
        }
        return "redirect:/borrow/active";
    }

    // Người dùng hủy yêu cầu trả truyện
    @PostMapping("/return/cancel/{returnRequestId}")
    public String cancelReturnRequest(@PathVariable Long returnRequestId,
                                      Authentication authentication,
                                      HttpServletRequest httpRequest,
                                      RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            borrowService.cancelReturnRequest(returnRequestId, user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã hủy yêu cầu trả truyện thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hủy yêu cầu thất bại: " + e.getMessage());
        }
        
        String referer = httpRequest.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/borrow");
    }
}
