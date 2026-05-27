package com.librarymanagementsystem.controller.admin;

import com.librarymanagementsystem.model.book.BookImportHistory;
import com.librarymanagementsystem.service.book.BookImportHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
public class AdminImportHistoryController {

    private final BookImportHistoryService bookImportHistoryService;

    @GetMapping("/import-history")
    public String viewImportHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookImportHistory> historyPage = bookImportHistoryService.getImportHistory(pageable);
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages());
        model.addAttribute("activePage", "importhistory");
        return "admin/import-history";
    }
}
