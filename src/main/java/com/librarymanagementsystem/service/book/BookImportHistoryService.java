package com.librarymanagementsystem.service.book;

import com.librarymanagementsystem.model.book.BookImportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookImportHistoryService {
    Page<BookImportHistory> getImportHistory(Pageable pageable);
}
