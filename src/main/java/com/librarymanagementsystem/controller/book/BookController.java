package com.librarymanagementsystem.controller.book;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.AuthorService;
import com.librarymanagementsystem.service.book.BookService;
import com.librarymanagementsystem.service.CategoryService;
import com.librarymanagementsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.librarymanagementsystem.service.book.BookReviewService;
import com.librarymanagementsystem.service.BorrowService;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final AuthorService authorService;
    private final UserService userService;
    private final BookReviewService bookReviewService;
    private final BorrowService borrowService;


    private void populateListModel(Model model, String title, Long category, int page, String sortBy, Authentication authentication) {
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
    }

    @GetMapping
    public String listBooks(@RequestParam(required = false) String title,
                            @RequestParam(required = false) Long category,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(required = false) String sortBy,
                            Model model,
                            Authentication authentication) {
        populateListModel(model, title, category, page, sortBy, authentication);
        
        if (!model.containsAttribute("bookDTO")) {
            model.addAttribute("bookDTO", new BookDTO());
        }
        if (!model.containsAttribute("editBookDTO")) {
            model.addAttribute("editBookDTO", new BookDTO());
        }

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

        // Load reviews & stats
        model.addAttribute("reviews", bookReviewService.getReviewsByBookId(id));
        model.addAttribute("averageRating", bookReviewService.getAverageRatingForBook(id));
        model.addAttribute("reviewCount", bookReviewService.countReviewsForBook(id));

        boolean hasRented = false;
        boolean hasPendingRequest = false;
        com.librarymanagementsystem.model.book.BookReview existingReview = null;
        // Kiểm tra xem user đã mượn truyện này chưa
        if (authentication != null && authentication.isAuthenticated()) {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElse(null);
            if (user != null) {
                boolean isBorrowed = bookService.isBookBorrowedByUser(id, user.getId());
                
                List<BorrowRequest> userRequests = borrowService.getUserBorrowRequests(user.getId());
                if (userRequests != null) {
                    for (BorrowRequest req : userRequests) {
                        if (req.getRequestStatus() == com.librarymanagementsystem.model.borrow.status.RequestStatus.PENDING) {
                            for (com.librarymanagementsystem.model.borrow.BorrowRequestItem item : req.getBorrowRequestItems()) {
                                if (item.getBook().getId().equals(id)) {
                                    hasPendingRequest = true;
                                    break;
                                }
                            }
                        }
                        if (hasPendingRequest) break;
                    }
                }
                
                model.addAttribute("isBookBorrowed", isBorrowed);
                model.addAttribute("userId", user.getId());
                hasRented = bookReviewService.hasUserRentedBook(user.getId(), id);
                existingReview = bookReviewService.getReviewByBookAndUser(id, user.getId()).orElse(null);
            }
        }
        model.addAttribute("hasRented", hasRented);
        model.addAttribute("hasPendingRequest", hasPendingRequest);
        model.addAttribute("existingReview", existingReview);
        return "book/book-detail";
    }

    @PostMapping("/{id}/review")
    @ResponseBody
    public ResponseEntity<?> submitReview(@PathVariable("id") Long bookId,
                                          @RequestParam("rating") Integer rating,
                                          @RequestParam("comment") String comment,
                                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Bạn cần đăng nhập để đánh giá!"));
        }
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            bookReviewService.saveReview(bookId, user.getId(), rating, comment);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cảm ơn bạn đã đánh giá truyện!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/add")
    public String showAddForm(Model model) {
        return "redirect:/books";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/save")
    public String saveBook(@Valid @ModelAttribute("bookDTO") BookDTO bookDTO, BindingResult bindingResult, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateListModel(model, null, null, 1, null, authentication);
            model.addAttribute("editBookDTO", new BookDTO());
            model.addAttribute("showAddModal", true);
            return "book/list";
        }
        try {
            bookService.saveBook(bookDTO);
            redirectAttributes.addFlashAttribute("message", "Thêm sách thành công");
        } catch (RuntimeException e) {
            populateListModel(model, null, null, 1, null, authentication);
            model.addAttribute("editBookDTO", new BookDTO());
            model.addAttribute("showAddModal", true);
            bindingResult.rejectValue("isbn", "error.bookDTO", e.getMessage());
            return "book/list";
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
    public String updateBook(@PathVariable Long id, @Valid @ModelAttribute("editBookDTO") BookDTO bookDTO, BindingResult bindingResult, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateListModel(model, null, null, 1, null, authentication);
            model.addAttribute("bookDTO", new BookDTO());
            model.addAttribute("showEditModal", true);
            model.addAttribute("editBookId", id);
            return "book/list";
        }
        try {
            bookService.updateBook(id, bookDTO);
            redirectAttributes.addFlashAttribute("message", "Cập nhật sách thành công");
        } catch (RuntimeException e) {
            populateListModel(model, null, null, 1, null, authentication);
            model.addAttribute("bookDTO", new BookDTO());
            model.addAttribute("showEditModal", true);
            model.addAttribute("editBookId", id);
            bindingResult.rejectValue("isbn", "error.editBookDTO", e.getMessage());
            return "book/list";
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

    @GetMapping("/api/check-isbn")
    @ResponseBody
    public ResponseEntity<Boolean> checkIsbn(@RequestParam String isbn, @RequestParam(required = false) Long currentId) {
        boolean exists = bookService.existsByIsbn(isbn);
        if (exists && currentId != null) {
            Optional<Book> book = bookService.getBookById(currentId);
            if (book.isPresent() && book.get().getIsbn().equals(isbn)) {
                return ResponseEntity.ok(false); // Not duplicated (it's the same book)
            }
        }
        return ResponseEntity.ok(exists);
    }
}
