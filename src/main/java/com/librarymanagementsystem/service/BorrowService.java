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

    List<BorrowRequest> getUserBorrowRequests(Long userId);

    Optional<BorrowRequest> getBorrowRequestDetail(Long requestId);

    void cancelBorrowRequest(Long requestId);

    BorrowTransaction approveBorrowRequest(Long requestId, Long librarianId, Integer borrowDays);

    void rejectBorrowRequest(Long requestId, String reason);

    List<BorrowHistoryDTO> getUserBorrowHistory(Long userId);

    Optional<BorrowTransaction> getBorrowTransactionDetail(Long transactionId);

    void returnBorrowItems(Long transactionId, Long librarianId);

    long calculateLateFine(Long transactionId);

    List<BorrowHistoryDTO> getActiveBorrows(Long userId);

    boolean isOverdue(Long transactionId);

    List<BorrowTransaction> getOverdueBooks(Long userId);

    List<BorrowRequest> getAllPendingRequests();

    List<BorrowTransaction> getAllActiveBorrows();

    void createReturnRequest(Long userId, Long transactionId, LocalDateTime returnDateTime, String note);

    List<ReturnRequest> getAllPendingReturnRequests();

    void approveReturnRequest(Long requestId, Long librarianId);
    void completeReturnRequest(Long requestId, Long librarianId);

    void rejectReturnRequest(Long requestId, String reason);
}
