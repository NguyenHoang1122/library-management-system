package com.librarymanagementsystem.repository.book;

import com.librarymanagementsystem.model.book.BookImportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookImportHistoryRepository extends JpaRepository<BookImportHistory, Long> {
    Page<BookImportHistory> findAllByOrderByImportDateDesc(Pageable pageable);
}
