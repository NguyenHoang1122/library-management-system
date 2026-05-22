package com.librarymanagementsystem.repository.book;

import com.librarymanagementsystem.model.book.BookCopy;
import com.librarymanagementsystem.model.book.status.BookCopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    Optional<BookCopy> findByBarcode(String barcode);
    List<BookCopy> findByBookId(Long bookId);
    List<BookCopy> findByBookIdAndStatus(Long bookId, BookCopyStatus status);
}
