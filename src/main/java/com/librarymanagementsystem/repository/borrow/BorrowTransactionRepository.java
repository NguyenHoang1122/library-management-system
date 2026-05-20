package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.status.TransactionStatus;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowTransactionRepository extends JpaRepository<BorrowTransaction, Long> {

    //giao dịch mượn theo user
    List<BorrowTransaction> findByUserOrderByBorrowDateDesc(User user);

    // mượn theo trạng thái giao dịch
//    List<BorrowTransaction> findByStatus(TransactionStatus status);

    //giao dịch mượn có trạng thái nằm trong danh sách các trạng thái truyền vào
    List<BorrowTransaction> findByStatusIn(List<TransactionStatus> statuses);

    // giao dịch mượn quá hạn của user
   @Query("SELECT bt FROM BorrowTransaction bt WHERE bt.user.id = :userId AND bt.status = 'BORROWED' AND bt.dueDate < CURRENT_TIMESTAMP")
    List<BorrowTransaction> findUserOverdueTransactions(@Param("userId") Long userId);

    //giao dịch mượn đang hoạt động của người dùng dựa trên đối tượng User và một trạng thái cụ thể
//    List<BorrowTransaction> findByUserAndStatus(User user, TransactionStatus status);

    //giao dịch mượn đang hoạt động của user
    List<BorrowTransaction> findByUserAndStatusInOrderByBorrowDateDesc(User user, List<TransactionStatus> statuses);



    // check user có đang mượn truyện or không
    @Query("SELECT COUNT(bi) > 0 FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bi.book.id = :bookId " +
            "AND bt.status IN ('BORROWED', 'OVERDUE', 'PENDING')")
    boolean isBookBorrowedByUser(@Param("userId") Long userId, @Param("bookId") Long bookId);

    //các ID của sách đang được mượn or đang yêu cầu mượn bởi user
    @Query("SELECT DISTINCT bi.book.id FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bt.status IN ('BORROWED', 'OVERDUE', 'PENDING')")
    List<Long> findBorrowedBookIdsByUser(@Param("userId") Long userId);

    // Check user đã từng thuê/mượn truyện này hay chưa
    @Query("SELECT COUNT(bi) > 0 FROM BorrowTransaction bt " +
            "JOIN bt.items bi " +
            "WHERE bt.user.id = :userId " +
            "AND bi.book.id = :bookId")
    boolean hasUserRentedBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
