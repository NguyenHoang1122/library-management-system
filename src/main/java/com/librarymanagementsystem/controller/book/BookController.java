package com.librarymanagementsystem.controller.book;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.Category;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.AuthorService;
import com.librarymanagementsystem.service.BookService;
import com.librarymanagementsystem.service.CategoryService;
import com.librarymanagementsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final AuthorService authorService;
    private final UserService userService;


    @GetMapping
    public String listBooks(@RequestParam(required = false) String title,
                            @RequestParam(required = false) Long category,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(required = false) String sortBy,
                            Model model,
                            Authentication authentication) {
        List<Book> books;
        if (title != null && !title.isEmpty()) {
            books = new ArrayList<>(bookService.searchBooks(title));
        } else if (category != null) {
            books = new ArrayList<>(bookService.getBooksByCategory(category));
        } else {
            books = new ArrayList<>(bookService.getAllBooks());
        }

        // Sắp xếp
        if ("author".equals(sortBy)) {
            books.sort((b1, b2) -> {
                String a1 = b1.getAuthor() != null ? b1.getAuthor().getName() : "";
                String a2 = b2.getAuthor() != null ? b2.getAuthor().getName() : "";
                return a1.compareToIgnoreCase(a2);
            });
        } else if ("category".equals(sortBy)) {
            books.sort((b1, b2) -> {
                String c1 = b1.getCategories().isEmpty() ? "" : b1.getCategories().iterator().next().getCategoryName();
                String c2 = b2.getCategories().isEmpty() ? "" : b2.getCategories().iterator().next().getCategoryName();
                return c1.compareToIgnoreCase(c2);
            });
        } else if ("quantity".equals(sortBy)) {
            books.sort((b1, b2) -> Integer.compare(b2.getQuantity(), b1.getQuantity()));
        }

        // Phân trang (10 phần tử mỗi trang)
        int pageSize = 10;
        int totalItems = books.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<Book> pagedBooks = books.subList(start, end);

        model.addAttribute("books", pagedBooks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("category", category);

        addUserDataToModel(model, authentication);

        boolean isAdminOrLibrarian = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isAdminOrLibrarian = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        }

        return isAdminOrLibrarian ? "book/list" : "book/user-list";
    }

    @GetMapping("/search")
    public String searchBooks(@RequestParam("query") String query,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(required = false) String sortBy,
                              Model model,
                              Authentication authentication) {
        List<Book> books = new ArrayList<>(bookService.searchBooks(query));

        // Sắp xếp
        if ("author".equals(sortBy)) {
            books.sort((b1, b2) -> {
                String a1 = b1.getAuthor() != null ? b1.getAuthor().getName() : "";
                String a2 = b2.getAuthor() != null ? b2.getAuthor().getName() : "";
                return a1.compareToIgnoreCase(a2);
            });
        } else if ("category".equals(sortBy)) {
            books.sort((b1, b2) -> {
                String c1 = b1.getCategories().isEmpty() ? "" : b1.getCategories().iterator().next().getCategoryName();
                String c2 = b2.getCategories().isEmpty() ? "" : b2.getCategories().iterator().next().getCategoryName();
                return c1.compareToIgnoreCase(c2);
            });
        } else if ("quantity".equals(sortBy)) {
            books.sort((b1, b2) -> Integer.compare(b2.getQuantity(), b1.getQuantity()));
        }

        // Phân trang
        int pageSize = 10;
        int totalItems = books.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        List<Book> pagedBooks = books.subList(start, end);

        model.addAttribute("books", pagedBooks);
        model.addAttribute("searchQuery", query);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("sortBy", sortBy);
        
        addUserDataToModel(model, authentication);

        boolean isAdminOrLibrarian = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isAdminOrLibrarian = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        }

        return isAdminOrLibrarian ? "book/list" : "book/user-list";
    }

    private void addUserDataToModel(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElse(null);
            if (user != null) {
                model.addAttribute("userId", user.getId());
            }
        }
    }


    @GetMapping("/{id}")
    public String viewBook(@PathVariable("id") Long id,
                           Model model,
                           Authentication authentication) {
        Book book = bookService.getBookById(id)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        model.addAttribute("book", book);

        // Kiểm tra xem user đã mượn truyện này chưa
        if (authentication != null && authentication.isAuthenticated()) {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElse(null);
            if (user != null) {
                boolean isBorrowed = bookService.isBookBorrowedByUser(id, user.getId());
                model.addAttribute("isBookBorrowed", isBorrowed);
                model.addAttribute("userId", user.getId());
            }
        }
        return "book/book-detail";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/add")
    public String showAddForm(Model model) {
        return "redirect:/books";
    }
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/save")
    public String saveBook(@ModelAttribute BookDTO bookDTO, RedirectAttributes redirectAttributes) {
        try {
            bookService.saveBook(bookDTO);
            redirectAttributes.addFlashAttribute("message", "Thêm sách thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        return "redirect:/books";
    }
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/update/{id}")
    public String updateBook(@PathVariable Long id, @ModelAttribute BookDTO bookDTO, RedirectAttributes redirectAttributes) {
        try {
            bookService.updateBook(id, bookDTO);
            redirectAttributes.addFlashAttribute("message", "Cập nhật sách thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("message", "Xóa sách thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }
}
