package com.librarymanagementsystem.service.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.*;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.borrow.status.TransactionStatus;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.BookRepository;
import com.librarymanagementsystem.repository.UserRepository;
import com.librarymanagementsystem.repository.borrow.*;
import com.librarymanagementsystem.service.BorrowService;
import com.librarymanagementsystem.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final ReturnRequestRepository returnRequestRepository;

    private final Integer DEFAULT_BORROW_DAYS = 14; // Mặc định mượn 14 ngày
    private final long DAILY_FINE = 5000; // 5000 VND per day

    @Override
    public BorrowRequest createBorrowRequest(Long userId, Long bookId, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Truyện không tồn tại"));

        if (book.getQuantity() <= 0) {
            throw new RuntimeException("Truyện đã hết trong kho, không thể mượn lúc này");
        }

        // TRỪ SỐ LƯỢNG NGAY KHI TẠO YÊU CẦU ĐỂ TRÁNH TRANH CHẤP
        book.setQuantity(book.getQuantity() - 1);
        bookRepository.save(book);

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

        // HOÀN LẠI SỐ LƯỢNG KHI HỦY
        List<BorrowRequestItem> items = borrowRequestItemRepository.findByBorrowRequest(borrowRequest);
        for (BorrowRequestItem item : items) {
            Book book = item.getBook();
            book.setQuantity(book.getQuantity() + 1);
            bookRepository.save(book);
        }

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

        // Thêm sách vào giao dịch (Số lượng đã trừ lúc tạo yêu cầu)
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

        // HOÀN LẠI SỐ LƯỢNG KHI TỪ CHỐI
        List<BorrowRequestItem> items = borrowRequestItemRepository.findByBorrowRequest(borrowRequest);
        for (BorrowRequestItem item : items) {
            Book book = item.getBook();
            book.setQuantity(book.getQuantity() + 1);
            bookRepository.save(book);
        }

        // Thông báo
        notificationService.notifyBorrowRejected(borrowRequest);
    }

    @Override
    public List<BorrowHistoryDTO> getUserBorrowHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        List<BorrowTransaction> transactions = borrowTransactionRepository.findByUser(user);
        return transactions.stream().map(this::mapToBorrowHistoryDTO).collect(Collectors.toList());
    }

    private BorrowHistoryDTO mapToBorrowHistoryDTO(BorrowTransaction transaction) {
        List<BorrowItem> items = borrowItemRepository.findByTransaction(transaction);
        Long bookId = items.isEmpty() ? null : items.get(0).getBook().getId();
        String bookName = items.isEmpty() ? "Không xác định" : items.get(0).getBook().getTitle();
        String bookImage = items.isEmpty() ? null : items.get(0).getBook().getImage();
        
        String returnRequestStatus = null;
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            List<ReturnRequest> requests = returnRequestRepository.findByBorrowTransactionOrderByRequestDateDesc(transaction);
            if (!requests.isEmpty()) {
                returnRequestStatus = requests.get(0).getRequestStatus().toString();
            }
        }

        return new BorrowHistoryDTO(
                transaction.getId(),
                bookId,
                bookName,
                bookImage,
                transaction.getBorrowDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                transaction.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                transaction.getReturnDate() != null ? transaction.getReturnDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Chưa trả",
                transaction.getStatus().toString(),
                returnRequestStatus
        );
    }

    @Override
    public Optional<BorrowTransaction> getBorrowTransactionDetail(Long transactionId) {
        return borrowTransactionRepository.findById(transactionId);
    }

    @Override
    public void returnBorrowItems(Long transactionId, Long librarianId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));
        User librarian = null;
        if (librarianId != null) {
            librarian = userRepository.findById(librarianId).orElseThrow(() -> new RuntimeException("Thủ thư không tồn tại"));
        }

        if (transaction.getStatus() == TransactionStatus.RETURNED) {
            throw new RuntimeException("Giao dịch này đã được hoàn thành trả truyện trước đó");
        }

        // Cập nhật thông tin trả và TĂNG số lượng trong kho
        transaction.setReturnDate(LocalDateTime.now());
        if (librarian != null) transaction.setLibrarian(librarian);

        transaction.setStatus(TransactionStatus.RETURNED);
        borrowTransactionRepository.save(transaction);

        // Tăng lại số lượng cho các sách trong giao dịch
        List<BorrowItem> items = borrowItemRepository.findByTransaction(transaction);
        for (BorrowItem item : items) {
            Book book = item.getBook();
            book.setQuantity(book.getQuantity() + 1);
            bookRepository.save(book);
        }

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
    public List<BorrowHistoryDTO> getActiveBorrows(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        List<TransactionStatus> activeStatuses = List.of(TransactionStatus.BORROWED, TransactionStatus.OVERDUE, TransactionStatus.PENDING);
        List<BorrowTransaction> transactions = borrowTransactionRepository.findByUserAndStatusIn(user, activeStatuses);
        return transactions.stream().map(this::mapToBorrowHistoryDTO).collect(Collectors.toList());
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
        return borrowTransactionRepository.findByStatusIn(List.of(TransactionStatus.BORROWED, TransactionStatus.OVERDUE, TransactionStatus.PENDING));
    }

    @Override
    public void createReturnRequest(Long userId, Long transactionId, LocalDateTime returnDateTime, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        if (transaction.getStatus() != TransactionStatus.BORROWED && transaction.getStatus() != TransactionStatus.OVERDUE) {
            throw new RuntimeException("Giao dịch không ở trạng thái có thể trả");
        }

        ReturnRequest request = new ReturnRequest();
        request.setUser(user);
        request.setBorrowTransaction(transaction);
        request.setRequestDate(LocalDateTime.now());
        request.setReturnDateTime(returnDateTime);
        request.setNote(note);
        request.setRequestStatus(RequestStatus.PENDING);

        returnRequestRepository.save(request);

        // Cập nhật trạng thái giao dịch sang PENDING
        transaction.setStatus(TransactionStatus.PENDING);
        borrowTransactionRepository.save(transaction);

        // Thông báo cho thủ thư
        notificationService.notifyReviewReturnRequest(request);
    }

    @Override
    public List<ReturnRequest> getAllPendingReturnRequests() {
        return returnRequestRepository.findByRequestStatusIn(List.of(RequestStatus.PENDING, RequestStatus.APPROVED));
    }

    @Override
    public void approveReturnRequest(Long requestId, Long librarianId) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        request.setRequestStatus(RequestStatus.APPROVED);
        returnRequestRepository.save(request);

        // Thông báo cho user là yêu cầu ĐÃ ĐƯỢC DUYỆT
        notificationService.notifyReturnApproved(request);
    }

    @Override
    @Transactional
    public void completeReturnRequest(Long requestId, Long librarianId) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        if (request.getRequestStatus() != RequestStatus.APPROVED) {
            throw new RuntimeException("Yêu cầu phải được duyệt trước khi xác nhận đã trả");
        }

        request.setRequestStatus(RequestStatus.COMPLETED);
        returnRequestRepository.save(request);

        // Hoàn tất việc trả sách (Cập nhật giao dịch sang RETURNED)
        returnBorrowItems(request.getBorrowTransaction().getId(), librarianId);
        
        // notifyBorrowReturned đã được gọi trong returnBorrowItems
    }

    @Override
    public void rejectReturnRequest(Long requestId, String reason) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        request.setRequestStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        returnRequestRepository.save(request);

        // Khôi phục trạng thái giao dịch (kiểm tra lại ngày hạn để đặt lại BORROWED hoặc OVERDUE)
        BorrowTransaction transaction = request.getBorrowTransaction();
        if (LocalDateTime.now().isAfter(transaction.getDueDate())) {
            transaction.setStatus(TransactionStatus.OVERDUE);
        } else {
            transaction.setStatus(TransactionStatus.BORROWED);
        }
        borrowTransactionRepository.save(transaction);
        
        // Thông báo
        notificationService.notifyReturnRejected(request);
    }
}
