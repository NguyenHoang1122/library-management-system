package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.status.TransactionStatus;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowTransactionRepository extends JpaRepository<BorrowTransaction, Long> {
    // Lấy danh sách giao dịch mượn của user
    List<BorrowTransaction> findByUser(User user);

    // Lấy danh sách giao dịch mượn theo trạng thái
    List<BorrowTransaction> findByStatus(TransactionStatus status);

    // Lấy danh sách giao dịch mượn quá hạn
    @Query("SELECT bt FROM BorrowTransaction bt WHERE bt.status = 'BORROWED' AND bt.dueDate < CURRENT_TIMESTAMP")
    List<BorrowTransaction> findOverdueTransactions();

    // Lấy danh sách giao dịch mượn quá hạn của user
    @Query("SELECT bt FROM BorrowTransaction bt WHERE bt.user.id = :userId AND bt.status = 'BORROWED' AND bt.dueDate < CURRENT_TIMESTAMP")
    List<BorrowTransaction> findUserOverdueTransactions(@Param("userId") Long userId);

    // Lấy giao dịch mượn đang hoạt động
    List<BorrowTransaction> findByUserAndStatus(User user, TransactionStatus status);

    // Lấy giao dịch theo ngày mượn
    @Query("SELECT bt FROM BorrowTransaction bt WHERE bt.user.id = :userId AND bt.borrowDate BETWEEN :startDate AND :endDate ORDER BY bt.borrowDate DESC")
    List<BorrowTransaction> findByUserAndDateRange(@Param("userId") Long userId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    // Count giao dịch đang mượn
    long countByStatus(TransactionStatus status);

    // Count giao dịch quá hạn
    @Query("SELECT COUNT(bt) FROM BorrowTransaction bt WHERE bt.status = 'BORROWED' AND bt.dueDate < CURRENT_TIMESTAMP")
    long countOverdueTransactions();
}
