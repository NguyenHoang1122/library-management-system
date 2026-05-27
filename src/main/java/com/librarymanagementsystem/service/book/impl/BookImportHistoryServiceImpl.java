package com.librarymanagementsystem.service.book.impl;

import com.librarymanagementsystem.model.book.BookImportHistory;
import com.librarymanagementsystem.repository.book.BookImportHistoryRepository;
import com.librarymanagementsystem.service.book.BookImportHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookImportHistoryServiceImpl implements BookImportHistoryService {

    private final BookImportHistoryRepository bookImportHistoryRepository;

    @Override
    public Page<BookImportHistory> getImportHistory(Pageable pageable) {
        return bookImportHistoryRepository.findAllByOrderByImportDateDesc(pageable);
    }
}
