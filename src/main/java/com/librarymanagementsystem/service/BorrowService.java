package com.librarymanagementsystem.service;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;

import java.util.List;
import java.util.Optional;

public interface BorrowService {

    BorrowRequest createBorrowRequest(Long userId, Long bookId, String note);

    List<BorrowRequest> getUserBorrowRequests(Long userId);

    Optional<BorrowRequest> getBorrowRequestDetail(Long requestId);

    void cancelBorrowRequest(Long requestId);

    BorrowTransaction approveBorrowRequest(Long requestId, Long librarianId, Integer borrowDays);

    void rejectBorrowRequest(Long requestId);

    List<BorrowTransaction> getUserBorrowHistory(Long userId);

    Optional<BorrowTransaction> getBorrowTransactionDetail(Long transactionId);

    void returnBorrowItems(Long transactionId, Long librarianId);

    long calculateLateFine(Long transactionId);

    List<BorrowTransaction> getActiveBorrows(Long userId);

    boolean isOverdue(Long transactionId);

    List<BorrowTransaction> getOverdueBooks(Long userId);

    List<BorrowRequest> getAllPendingRequests();

    List<BorrowTransaction> getAllActiveBorrows();
}
