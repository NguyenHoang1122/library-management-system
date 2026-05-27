package com.librarymanagementsystem.service.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;
import com.librarymanagementsystem.model.borrow.dto.CombinedHistoryDTO;
import com.librarymanagementsystem.model.borrow.status.DeliveryMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BorrowService {
    
    BorrowRequest checkout(Long userId, DeliveryMethod deliveryMethod, String shippingAddress, String note);

    BorrowRequest createBorrowRequest(Long userId, Long bookId, String note);

    List<Long> getBorrowedBookIdsByUser(Long userId);

    //danh sách yêu cầu mượn của user
    List<BorrowRequest> getUserBorrowRequests(Long userId);

    Page<BorrowRequest> getUserBorrowRequests(Long userId, Pageable pageable);

    // chi tiết của một yêu cầu mượn truyện
    Optional<BorrowRequest> getBorrowRequestDetail(Long requestId);

    //tự hủy yêu cầu mượn sách của mình khi đang chờ duyệt
    void cancelBorrowRequest(Long requestId);

    // Thủ thư duyệt yêu cầu mượn truyện (chuyển sang trạng thái Đang Gửi/Chờ nhận)
    void approveBorrowRequest(Long requestId, Long librarianId, Integer borrowDays);

    // Thủ thư xác nhận đã giao sách (chuyển đơn sang Đang mượn)
    BorrowTransaction completeBorrowDelivery(Long requestId, Long librarianId);

    // Thủ thư từ chối yêu cầu mượn truyện
    void rejectBorrowRequest(Long requestId, String reason);

    // toàn bộ lịch sử giao dịch mượn trả sách của user
    Page<BorrowHistoryDTO> getUserBorrowHistory(Long userId, Pageable pageable);

    Page<CombinedHistoryDTO> getCombinedBorrowHistory(Long userId, Pageable pageable);

    //chi tiết thông tin của một giao dịch mượn trả
    Optional<BorrowTransaction> getBorrowTransactionDetail(Long transactionId);

    // Thủ thư xác nhận người dùng trả sách trực tiếp tại quầy
    void returnBorrowItems(Long transactionId, Long librarianId, List<Long> itemIds, Double returnShippingFee);

    // Tính tiền phạt
    long calculateLateFine(Long transactionId);

    // Thủ thư tạo đơn mượn trực tiếp tại quầy
    void createDirectBorrow(Long librarianId, Long userId, List<Long> bookIds, List<Integer> quantities);

    // Danh sách đang mượn
    Page<BorrowHistoryDTO> getActiveBorrows(Long userId, Pageable pageable);

    Page<BorrowTransaction> getActiveTransactionsPaged(Long userId, Pageable pageable);

    // Check mượn quá hạn chưa
    boolean isOverdue(Long transactionId);

    // Gia hạn sách
    void extendBorrowTransaction(Long transactionId);

    // Mượn quá hạn
    List<BorrowTransaction> getOverdueBooks(Long userId);

    // Danh sách truyện chờ duyệt
    List<BorrowRequest> getAllPendingRequests();

    org.springframework.data.domain.Page<BorrowRequest> getPendingRequests(String query, Pageable pageable);

    org.springframework.data.domain.Page<BorrowRequest> getApprovedRequests(String query, Pageable pageable);

    // Danh sách truyện đang mượn
    List<BorrowTransaction> getAllActiveBorrows();

    org.springframework.data.domain.Page<Object[]> getActiveBorrowers(String query, Pageable pageable);

    // User tạo yêu cầu trả sách trực tuyến
    void createReturnRequest(Long userId, Long transactionId, LocalDateTime returnDateTime, String note);
    void createPartialReturnRequest(Long userId, Long requestId, Map<Long, Integer> returnItems, String returnMethod, String returnAddress);
    void extendBorrowRequest(Long userId, Long requestId);
    ReturnRequest getReturnRequestForBorrowRequest(Long requestId);
    List<ReturnRequest> getAllReturnRequestsForBorrowRequest(Long requestId);
    BorrowTransaction getTransactionForBorrowRequest(Long requestId);

    // Danh sách yêu cầu trả chờ duyệt
    List<ReturnRequest> getAllPendingReturnRequests();

    Page<ReturnRequest> getPendingReturnRequests(String query, Pageable pageable);

    // Thủ thư duyệt yêu cầu trả sách (Shipper bắt đầu đi lấy)
    void approveReturnRequest(Long requestId, Long librarianId);

    // Shipper đã lấy được hàng từ tay người dùng
    void receiveReturnRequest(Long requestId, Long librarianId);

    // Hoàn tất yêu cầu trả sách (đã đem về thư viện)
    void completeReturnRequest(Long requestId, Long librarianId);

    // Thủ thư từ chối yêu cầu trả sách
    void rejectReturnRequest(Long requestId, String reason);
    
    // User hủy yêu cầu trả sách
    void cancelReturnRequest(Long requestId, Long userId);

    org.springframework.data.domain.Page<com.librarymanagementsystem.model.borrow.dto.CombinedHistoryDTO> getCombinedBorrowHistory(Long userId, String keyword, Long bookId, String statusFilter, String sortOption, org.springframework.data.domain.Pageable pageable);

    java.util.Map<String, Object> getBorrowRequestDetailForUser(Long requestId, Long userId);

    java.util.List<java.util.Map<String, Object>> getReturnRequestDetails(Long requestId);

    java.util.List<java.util.Map<String, Object>> getActiveBooksGrouped(Long userId);

    java.util.List<java.util.Map<String, Object>> getGroupedItemsForTransaction(Long transactionId);

    java.util.List<Long> getPendingBookIdsByUser(Long userId);
}
