package com.librarymanagementsystem.controller.borrow;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.BorrowItem;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;
import com.librarymanagementsystem.model.borrow.dto.CombinedHistoryDTO;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.book.BookService;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.service.user.UserService;
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
        
        List<CombinedHistoryDTO> allHistory = new ArrayList<>(borrowService.getCombinedBorrowHistory(user.getId(), Pageable.unpaged()).getContent());
        
        // Filtering
        if (bookId != null || (keyword != null && !keyword.trim().isEmpty())) {
            String lowerKw = keyword != null ? keyword.toLowerCase() : "";
            allHistory = allHistory.stream().filter(dto -> {
                boolean matchBookId = false;
                if (bookId != null && dto.getBooksSummary() != null) {
                    try {
                        String bookTitle = bookService.getBookById(bookId).get().getTitle();
                        matchBookId = dto.getBooksSummary().contains(bookTitle);
                    } catch (Exception e) {}
                }
                
                boolean matchKeyword = false;
                if (keyword != null && !keyword.trim().isEmpty() && dto.getBooksSummary() != null) {
                    matchKeyword = dto.getBooksSummary().toLowerCase().contains(lowerKw);
                }
                
                if (bookId != null && (keyword == null || keyword.trim().isEmpty())) {
                    return matchBookId;
                } else if (bookId == null && keyword != null && !keyword.trim().isEmpty()) {
                    return matchKeyword;
                } else {
                    return matchBookId || matchKeyword;
                }
            }).collect(java.util.stream.Collectors.toList());
        }
        
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            allHistory = allHistory.stream().filter(dto -> dto.getStatus().equals(statusFilter)).collect(Collectors.toList());
        }
        
        // Sorting
        if (sortOption != null) {
            switch(sortOption) {
                case "newest":
                    allHistory.sort((a,b) -> b.getRequestId().compareTo(a.getRequestId()));
                    break;
                case "return_date":
                    allHistory.sort((a,b) -> {
                        if(a.getReturnDate().equals("Chưa có")) return 1;
                        if(b.getReturnDate().equals("Chưa có")) return -1;
                        try {
                            LocalDate d1 = LocalDate.parse(a.getReturnDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            LocalDate d2 = LocalDate.parse(b.getReturnDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            return d2.compareTo(d1); // Nearest return date
                        } catch(Exception e) { return 0; }
                    });
                    break;
            }
        } else {
            // Default sort by request date
            allHistory.sort((a,b) -> b.getRequestId().compareTo(a.getRequestId()));
        }

        int pageSize = 10;
        int totalItems = allHistory.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<CombinedHistoryDTO> paginated = new ArrayList<>();
        if (start < totalItems) {
            paginated = allHistory.subList(start, end);
        }

        long totalActive = borrowService.getActiveBorrows(user.getId(), PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("borrowHistory", paginated);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalBorrows", totalItems);
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

        BorrowRequest request = borrowService.getBorrowRequestDetail(requestId)
                .orElseThrow(() -> new RuntimeException("Đơn mượn không tồn tại"));

        if (!request.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền xem thông tin này");
        }

        ReturnRequest returnRequest = borrowService.getReturnRequestForBorrowRequest(requestId);
        BorrowTransaction transaction = borrowService.getTransactionForBorrowRequest(requestId);
        
        if (returnRequest != null && transaction != null) {
            double originalDeposit = 0.0;
            int returnQuantity = 0;
            if (returnRequest.getReturnItems() != null) {
                for (com.librarymanagementsystem.model.borrow.ReturnRequestItem item : returnRequest.getReturnItems()) {
                    if (item.getBook() != null && item.getBook().getDepositPrice() != null) {
                        originalDeposit += item.getBook().getDepositPrice() * item.getQuantity();
                    }
                    returnQuantity += item.getQuantity();
                }
            }
            
            double totalBorrowFee = returnQuantity * 5000.0;
            long lateFine = borrowService.calculateLateFine(transaction.getId());
            double returnShippingFee = returnRequest.getShippingFee() != null ? returnRequest.getShippingFee() : 0.0;
            
            double refundAmount = originalDeposit - totalBorrowFee - lateFine - returnShippingFee;
            if (refundAmount < 0) refundAmount = 0.0;
            
            model.addAttribute("returnOriginalDeposit", originalDeposit);
            model.addAttribute("returnBorrowFee", totalBorrowFee);
            model.addAttribute("returnLateFine", lateFine);
            model.addAttribute("returnRefundAmount", refundAmount);
        }
        
        java.util.Map<Long, Integer> returnedQuantities = new java.util.HashMap<>();
        java.util.Map<Long, Integer> unreturnedQuantities = new java.util.HashMap<>();
        if (transaction != null) {
            for (com.librarymanagementsystem.model.borrow.BorrowItem bi : transaction.getItems()) {
                Long bookId = bi.getBookCopy().getBook().getId();
                if (bi.getReturnDate() != null) {
                    returnedQuantities.put(bookId, returnedQuantities.getOrDefault(bookId, 0) + 1);
                } else {
                    unreturnedQuantities.put(bookId, unreturnedQuantities.getOrDefault(bookId, 0) + 1);
                }
            }
        }
        model.addAttribute("returnedQuantities", returnedQuantities);
        model.addAttribute("unreturnedQuantities", unreturnedQuantities);
        
        model.addAttribute("request", request);
        model.addAttribute("returnRequest", returnRequest);
        model.addAttribute("transaction", transaction);
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
        
        List<BorrowTransaction> activeTxs = borrowService.getActiveTransactionsPaged(user.getId(), Pageable.unpaged()).getContent();
        
        Map<Long, Map<String, Object>> groupedBooks = new HashMap<>();
        for (BorrowTransaction tx : activeTxs) {
            for (BorrowItem item : tx.getItems()) {
                if (item.getReturnDate() == null) {
                    Long bookId = item.getBookCopy().getBook().getId();
                    if (!groupedBooks.containsKey(bookId)) {
                        Map<String, Object> group = new HashMap<>();
                        group.put("book", item.getBookCopy().getBook());
                        group.put("quantity", 0);
                        groupedBooks.put(bookId, group);
                    }
                    Map<String, Object> group = groupedBooks.get(bookId);
                    group.put("quantity", (int)group.get("quantity") + 1);
                }
            }
        }

        List<Map<String, Object>> activeBooks = new ArrayList<>(groupedBooks.values());

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

        Map<Long, Map<String, Object>> grouped = new HashMap<>();
        for (BorrowItem item : transaction.getItems()) {
            Long bookId = item.getBookCopy().getBook().getId();
            if (!grouped.containsKey(bookId)) {
                java.util.Map<String, Object> groupInfo = new java.util.HashMap<>();
                groupInfo.put("book", item.getBookCopy().getBook());
                groupInfo.put("totalCount", 0);
                groupInfo.put("returnedCount", 0);
                groupInfo.put("unreturnedIds", new ArrayList<Long>());
                grouped.put(bookId, groupInfo);
            }
            Map<String, Object> groupInfo = grouped.get(bookId);
            groupInfo.put("totalCount", (int) groupInfo.get("totalCount") + 1);
            if (item.getReturnDate() != null) {
                groupInfo.put("returnedCount", (int) groupInfo.get("returnedCount") + 1);
            } else {
                ((List<Long>) groupInfo.get("unreturnedIds")).add(item.getId());
            }
        }
        model.addAttribute("groupedItems", new ArrayList<>(grouped.values()));

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
            java.util.Map<Long, Integer> returnItems = new java.util.HashMap<>();
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
}
