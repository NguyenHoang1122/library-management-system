package com.librarymanagementsystem.service;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BorrowService {

    BorrowRequest createBorrowRequest(Long userId, Long bookId, String note);

    //danh sách yêu cầu mượn của user
    List<BorrowRequest> getUserBorrowRequests(Long userId);

    // chi tiết của một yêu cầu mượn truyện
    Optional<BorrowRequest> getBorrowRequestDetail(Long requestId);

    //tự hủy yêu cầu mượn sách của mình khi đang chờ duyệt
    void cancelBorrowRequest(Long requestId);

    // Thủ thư duyệt yêu cầu mượn truyện
    BorrowTransaction approveBorrowRequest(Long requestId, Long librarianId, Integer borrowDays);

    // Thủ thư từ chối yêu cầu mượn truyện
    void rejectBorrowRequest(Long requestId, String reason);

    // toàn bộ lịch sử giao dịch mượn trả sách của user
    List<BorrowHistoryDTO> getUserBorrowHistory(Long userId);

    //chi tiết thông tin của một giao dịch mượn trả
    Optional<BorrowTransaction> getBorrowTransactionDetail(Long transactionId);

    // Thủ thư xác nhận người dùng trả sách trực tiếp tại quầy
    void returnBorrowItems(Long transactionId, Long librarianId);

    // Tính tiền phạt
    long calculateLateFine(Long transactionId);

    // Danh sách đang mượn
    List<BorrowHistoryDTO> getActiveBorrows(Long userId);

    // Check mượn quá hạn chưa
    boolean isOverdue(Long transactionId);

    // Mượn quá hạn
    List<BorrowTransaction> getOverdueBooks(Long userId);

    // Danh sách truyện chờ duyệt
    List<BorrowRequest> getAllPendingRequests();

    // Danh sách truyện đang mượn
    List<BorrowTransaction> getAllActiveBorrows();

    // User tạo yêu cầu trả sách trực tuyến
    void createReturnRequest(Long userId, Long transactionId, LocalDateTime returnDateTime, String note);

    // Danh sách yêu cầu trả chờ duyệt
    List<ReturnRequest> getAllPendingReturnRequests();

    // Thủ thư duyệt yêu cầu trả sách
    void approveReturnRequest(Long requestId, Long librarianId);

    // Hoàn tất yêu cầu trả sách
    void completeReturnRequest(Long requestId, Long librarianId);

    // Thủ thư từ chối yêu cầu trả sách
    void rejectReturnRequest(Long requestId, String reason);
}
