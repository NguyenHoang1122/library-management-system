package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.BorrowItem;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowItemRepository extends JpaRepository<BorrowItem, Long> {

    // Lấy danh sách sách trong giao dịch mượn
    List<BorrowItem> findByTransaction(BorrowTransaction transaction);

    // Kiểm tra sách có trong giao dịch mượn không
    Optional<BorrowItem> findByTransactionAndBook(BorrowTransaction transaction, Book book);

    // Count sách trong giao dịch mượn
    long countByTransaction(BorrowTransaction transaction);
}
