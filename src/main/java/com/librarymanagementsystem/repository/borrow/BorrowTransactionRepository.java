package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.status.TransactionStatus;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowTransactionRepository extends JpaRepository<BorrowTransaction, Long> {

    //giao dịch mượn theo user
    List<BorrowTransaction> findByUserOrderByBorrowDateDesc(User user);

    Page<BorrowTransaction> findByUser(User user, Pageable pageable);

    // mượn theo trạng thái giao dịch
//    List<BorrowTransaction> findByStatus(TransactionStatus status);
    
    List<BorrowTransaction> findByStatusAndDueDateBetween(TransactionStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    //giao dịch mượn có trạng thái nằm trong danh sách các trạng thái truyền vào
    List<BorrowTransaction> findByStatusIn(List<TransactionStatus> statuses);

    // giao dịch mượn quá hạn của user
   @Query("SELECT bt FROM BorrowTransaction bt WHERE bt.user.id = :userId AND bt.status = 'BORROWED' AND bt.dueDate < CURRENT_TIMESTAMP")
    List<BorrowTransaction> findUserOverdueTransactions(@Param("userId") Long userId);

    //giao dịch mượn đang hoạt động của người dùng dựa trên đối tượng User và một trạng thái cụ thể
//    List<BorrowTransaction> findByUserAndStatus(User user, TransactionStatus status);

    //giao dịch mượn đang hoạt động của user
    List<BorrowTransaction> findByUserAndStatusInOrderByBorrowDateDesc(User user, List<TransactionStatus> statuses);

    Page<BorrowTransaction> findByUserAndStatusIn(User user, List<TransactionStatus> statuses, Pageable pageable);



    // check user có đang mượn truyện or không
    @Query("SELECT COUNT(bi) > 0 FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bi.bookCopy.book.id = :bookId " +
            "AND bt.status IN ('BORROWED', 'OVERDUE', 'PENDING')")
    boolean isBookBorrowedByUser(@Param("userId") Long userId, @Param("bookId") Long bookId);

    //các ID của sách đang được mượn or đang yêu cầu mượn bởi user
    @Query("SELECT DISTINCT bi.bookCopy.book.id FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bt.status IN ('BORROWED', 'OVERDUE', 'PENDING')")
    List<Long> findBorrowedBookIdsByUser(@Param("userId") Long userId);

    // Check user đã từng thuê/mượn truyện này hay chưa
    @Query("SELECT COUNT(bi) > 0 FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bi.bookCopy.book.id = :bookId")
    boolean hasUserRentedBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("SELECT bt.user.id AS userId, bt.user.fullName AS fullName, bt.user.userName AS userName, bt.user.email AS email, COUNT(bi.id) AS borrowCount " +
           "FROM BorrowTransaction bt " +
           "JOIN bt.items bi " +
           "WHERE bt.status IN (com.librarymanagementsystem.model.borrow.status.TransactionStatus.BORROWED, com.librarymanagementsystem.model.borrow.status.TransactionStatus.OVERDUE, com.librarymanagementsystem.model.borrow.status.TransactionStatus.PENDING) " +
           "AND bi.returnDate IS NULL " +
           "AND (:query IS NULL OR :query = '' OR " +
           "     LOWER(bt.user.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(bt.user.userName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(bt.user.email) LIKE LOWER(CONCAT('%', :query, '%')))" +
           "GROUP BY bt.user.id, bt.user.fullName, bt.user.userName, bt.user.email " +
           "ORDER BY COUNT(bi.id) DESC")
    Page<Object[]> findActiveBorrowers(@Param("query") String query, Pageable pageable);
}
