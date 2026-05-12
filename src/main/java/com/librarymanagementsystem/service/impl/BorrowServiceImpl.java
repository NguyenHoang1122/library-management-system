package com.librarymanagementsystem.service.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.BorrowItem;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowRequestItem;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.borrow.status.TransactionStatus;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.BookRepository;
import com.librarymanagementsystem.repository.UserRepository;
import com.librarymanagementsystem.repository.borrow.BorrowItemRepository;
import com.librarymanagementsystem.repository.borrow.BorrowRequestItemRepository;
import com.librarymanagementsystem.repository.borrow.BorrowRequestRepository;
import com.librarymanagementsystem.repository.borrow.BorrowTransactionRepository;
import com.librarymanagementsystem.service.BorrowService;
import com.librarymanagementsystem.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final BorrowRequestItemRepository borrowRequestItemRepository;
    private final BorrowItemRepository borrowItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final NotificationService notificationService;

    private final Integer DEFAULT_BORROW_DAYS = 14; // Mặc định mượn 14 ngày
    private final long DAILY_FINE = 5000; // 5000 VND per day

    @Override
    public BorrowRequest createBorrowRequest(Long userId, Long bookId, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        // Tạo yêu cầu mượn
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setUser(user);
        borrowRequest.setRequestDate(LocalDateTime.now());
        borrowRequest.setRequestStatus(RequestStatus.PENDING);
        borrowRequest.setNote(note);
        borrowRequest = borrowRequestRepository.save(borrowRequest);

        // Thêm sách vào yêu cầu
        BorrowRequestItem borrowRequestItem = new BorrowRequestItem();
        borrowRequestItem.setBorrowRequest(borrowRequest);
        borrowRequestItem.setBook(book);
        borrowRequestItem.setQuantity(1);
        borrowRequestItemRepository.save(borrowRequestItem);

        // Tạo thông báo cho thủ thư
        notificationService.notifyReviewBorrowRequest(borrowRequest);

        return borrowRequest;
    }

    @Override
    public List<BorrowRequest> getUserBorrowRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return borrowRequestRepository.findByUser(user);
    }

    @Override
    public Optional<BorrowRequest> getBorrowRequestDetail(Long requestId) {
        return borrowRequestRepository.findById(requestId);
    }

    @Override
    public void cancelBorrowRequest(Long requestId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));

        if (borrowRequest.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy yêu cầu chưa duyệt");
        }

        borrowRequest.setRequestStatus(RequestStatus.CANCELLED);
        borrowRequestRepository.save(borrowRequest);

        // Thông báo
        notificationService.notifyBorrowRequestCancelled(borrowRequest);
    }

    @Override
    public BorrowTransaction approveBorrowRequest(Long requestId, Long librarianId, Integer borrowDays) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Thủ thư không tồn tại"));

        if (borrowRequest.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu mượn không ở trạng thái chưa duyệt");
        }

        // Tạo giao dịch mượn
        BorrowTransaction transaction = new BorrowTransaction();
        transaction.setUser(borrowRequest.getUser());
        transaction.setLibrarian(librarian);
        transaction.setBorrowDate(LocalDateTime.now());

        // Tính ngày trả
        if (borrowDays == null || borrowDays <= 0) {
            borrowDays = DEFAULT_BORROW_DAYS;
        }
        transaction.setDueDate(LocalDateTime.now().plusDays(borrowDays));
        transaction.setStatus(TransactionStatus.BORROWED);
        transaction = borrowTransactionRepository.save(transaction);

        // Thêm sách vào giao dịch
        List<BorrowRequestItem> requestItems = borrowRequestItemRepository.findByBorrowRequest(borrowRequest);
        for (BorrowRequestItem requestItem : requestItems) {
            BorrowItem borrowItem = new BorrowItem();
            borrowItem.setTransaction(transaction);
            borrowItem.setBook(requestItem.getBook());
            borrowItemRepository.save(borrowItem);
        }

        // Cập nhật trạng thái yêu cầu
        borrowRequest.setRequestStatus(RequestStatus.APPROVED);
        borrowRequestRepository.save(borrowRequest);

        // Thông báo cho user
        notificationService.notifyBorrowApproved(transaction);

        return transaction;
    }

    @Override
    public void rejectBorrowRequest(Long requestId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));

        if (borrowRequest.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể từ chối yêu cầu chưa duyệt");
        }

        borrowRequest.setRequestStatus(RequestStatus.REJECTED);
        borrowRequestRepository.save(borrowRequest);

        // Thông báo
        notificationService.notifyBorrowRejected(borrowRequest);
    }

    @Override
    public List<BorrowTransaction> getUserBorrowHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return borrowTransactionRepository.findByUser(user);
    }

    @Override
    public Optional<BorrowTransaction> getBorrowTransactionDetail(Long transactionId) {
        return borrowTransactionRepository.findById(transactionId);
    }

    @Override
    public void returnBorrowItems(Long transactionId, Long librarianId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Thủ thư không tồn tại"));

        if (transaction.getStatus() != TransactionStatus.BORROWED) {
            throw new RuntimeException("Giao dịch này không ở trạng thái đang mượn");
        }

        // Cập nhật thông tin trả
        transaction.setReturnDate(LocalDateTime.now());
        transaction.setLibrarian(librarian);

        // Kiểm tra quá hạn
        if (transaction.getReturnDate().isAfter(transaction.getDueDate())) {
            transaction.setStatus(TransactionStatus.OVERDUE);
        } else {
            transaction.setStatus(TransactionStatus.RETURNED);
        }

        borrowTransactionRepository.save(transaction);

        // Thông báo
        notificationService.notifyBorrowReturned(transaction);
    }

    @Override
    public long calculateLateFine(Long transactionId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        if (transaction.getReturnDate() == null ||
                (transaction.getStatus() != TransactionStatus.OVERDUE && transaction.getStatus() != TransactionStatus.RETURNED)) {
            return 0;
        }

        LocalDateTime returnDate = transaction.getReturnDate();
        long daysOverdue = ChronoUnit.DAYS.between(transaction.getDueDate(), returnDate);

        if (daysOverdue > 0) {
            return daysOverdue * DAILY_FINE;
        }

        return 0;
    }

    @Override
    public List<BorrowTransaction> getActiveBorrows(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return borrowTransactionRepository.findByUserAndStatus(user, TransactionStatus.BORROWED);
    }

    @Override
    public boolean isOverdue(Long transactionId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        if (transaction.getStatus() != TransactionStatus.BORROWED) {
            return false;
        }

        return LocalDateTime.now().isAfter(transaction.getDueDate());
    }

    @Override
    public List<BorrowTransaction> getOverdueBooks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return borrowTransactionRepository.findUserOverdueTransactions(userId);
    }

    @Override
    public List<BorrowRequest> getAllPendingRequests() {
        return borrowRequestRepository.findPendingRequests();
    }

    @Override
    public List<BorrowTransaction> getAllActiveBorrows() {
        return borrowTransactionRepository.findByStatus(TransactionStatus.BORROWED);
    }
}
