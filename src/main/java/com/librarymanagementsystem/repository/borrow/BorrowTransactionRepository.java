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
    // Lấy danh sách giao dịch mượn của user, sắp xếp từ gần nhất
    List<BorrowTransaction> findByUserOrderByBorrowDateDesc(User user);

    // Lấy danh sách giao dịch mượn theo trạng thái
    List<BorrowTransaction> findByStatus(TransactionStatus status);

    List<BorrowTransaction> findByStatusIn(List<TransactionStatus> statuses);

    // Lấy danh sách giao dịch mượn quá hạn của user
    @Query("SELECT bt FROM BorrowTransaction bt WHERE bt.user.id = :userId AND bt.status = 'BORROWED' AND bt.dueDate < CURRENT_TIMESTAMP")
    List<BorrowTransaction> findUserOverdueTransactions(@Param("userId") Long userId);

    // Lấy giao dịch mượn đang hoạt động
    List<BorrowTransaction> findByUserAndStatus(User user, TransactionStatus status);

    // Lấy giao dịch mượn đang hoạt động của user, sắp xếp từ gần nhất
    List<BorrowTransaction> findByUserAndStatusInOrderByBorrowDateDesc(User user, List<TransactionStatus> statuses);



    // Kiểm tra xem user có đang mượn sách này hay không (chỉ trạng thái BORROWED)
    @Query("SELECT COUNT(bi) > 0 FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bi.book.id = :bookId " +
            "AND bt.status IN ('BORROWED', 'OVERDUE', 'PENDING')")
    boolean isBookBorrowedByUser(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("SELECT DISTINCT bi.book.id FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bt.status IN ('BORROWED', 'OVERDUE', 'PENDING')")
    List<Long> findBorrowedBookIdsByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(bi) > 0 FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bi.book.id = :bookId")
    boolean hasUserRentedBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
