package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRequestItemRepository extends JpaRepository<BorrowRequestItem, Long> {

    // Lấy danh sách sách trong yêu cầu mượn
    List<BorrowRequestItem> findByBorrowRequest(BorrowRequest borrowRequest);

    // Kiểm tra sách có trong yêu cầu mượn không
    Optional<BorrowRequestItem> findByBorrowRequestAndBook(BorrowRequest borrowRequest, Book book);

    // Count sách trong yêu cầu mượn
    long countByBorrowRequest(BorrowRequest borrowRequest);
}
