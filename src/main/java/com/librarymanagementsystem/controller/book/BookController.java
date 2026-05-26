package com.librarymanagementsystem.controller.book;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.BookComment;
import com.librarymanagementsystem.model.book.BookReview;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import com.librarymanagementsystem.model.borrow.BorrowRequestItem;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.book.BookService;
import com.librarymanagementsystem.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.librarymanagementsystem.service.book.BookReviewService;
import com.librarymanagementsystem.service.book.BookCommentService;
import com.librarymanagementsystem.service.borrow.BorrowService;
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
    private final UserService userService;
    private final BookReviewService bookReviewService;
    private final BookCommentService bookCommentService;
    private final BorrowService borrowService;

    // Hỗ trợ điền danh sách truyện
    private void populateListModel(Model model, String title, Long category,
                                   int page, String sortBy, Authentication authentication) {
        Sort sort = Sort.unsorted();
        if ("author".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "author.name");
        } else if ("category".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "categories.categoryName");
        } else if ("quantity".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "quantity");
        }

        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10, sort);

        Page<Book> bookPage;
        if (title != null && !title.isEmpty()) {
            bookPage = bookService.searchBooks(title, pageable);
        } else if (category != null) {
            bookPage = bookService.getBooksByCategory(category, pageable);
        } else {
            bookPage = bookService.getAllBooks(pageable);
        }

        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("currentPage", bookPage.getNumber() + 1);
        model.addAttribute("totalPages", bookPage.getTotalPages() > 0 ? bookPage.getTotalPages() : 1);
        model.addAttribute("totalItems", bookPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("category", category);

        addUserDataToModel(model, authentication);
    }

    // Danh sách truyện
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

    //tìm kiếm truyện tiêu đề hoặc tên tác giả
    @GetMapping("/search")
    public String searchBooks(@RequestParam("query") String query,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(required = false) String sortBy,
                              Model model,
                              Authentication authentication) {
        Sort sort = Sort.unsorted();
        if ("author".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "author.name");
        } else if ("category".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "categories.categoryName");
        } else if ("quantity".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "quantity");
        }

        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10, sort);

        Page<Book> bookPage = bookService.searchBooks(query, pageable);

        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("searchQuery", query);
        model.addAttribute("currentPage", bookPage.getNumber() + 1);
        model.addAttribute("totalPages", bookPage.getTotalPages() > 0 ? bookPage.getTotalPages() : 1);
        model.addAttribute("totalItems", bookPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        
        addUserDataToModel(model, authentication);

        boolean isAdminOrLibrarian = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isAdminOrLibrarian = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        }

        return isAdminOrLibrarian ? "book/list" : "book/user-list";
    }

    // đưa tt user vào model
    private void addUserDataToModel(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElse(null);
            if (user != null) {
                model.addAttribute("userId", user.getId());
            }
        }
    }


    //chi tiết sách
    @GetMapping("/{id}")
    public String viewBook(@PathVariable("id") Long id,
                           @RequestParam(value = "commentPage", defaultValue = "0") int commentPage,
                           Model model,
                           Authentication authentication) {
        Book book = bookService.getBookById(id)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        model.addAttribute("book", book);

        model.addAttribute("reviews", bookReviewService.getReviewsByBookId(id));
        model.addAttribute("averageRating", bookReviewService.getAverageRatingForBook(id));
        model.addAttribute("reviewCount", bookReviewService.countReviewsForBook(id));
        model.addAttribute("ratingSummary", bookReviewService.getRatingSummary(id));

        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            currentUser = userService.findByUserName(authentication.getName()).orElse(null);
        }
        boolean isAdminOrLibrarian = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        
        Page<BookComment> commentPageResult = bookCommentService.getCommentsForBook(id, isAdminOrLibrarian, currentUser, commentPage, 6);
        model.addAttribute("commentsPage", commentPageResult);
        model.addAttribute("comments", commentPageResult.getContent());

        boolean hasRented = false;
        boolean hasPendingRequest = false;
        BookReview existingReview = null;
        // Check user đã mượn truyện này chưa
        if (authentication != null && authentication.isAuthenticated()) {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElse(null);
            if (user != null) {
                boolean isBorrowed = bookService.isBookBorrowedByUser(id, user.getId());
                
                List<BorrowRequest> userRequests = borrowService.getUserBorrowRequests(user.getId());
                if (userRequests != null) {
                    for (BorrowRequest req : userRequests) {
                        if (req.getRequestStatus() == RequestStatus.PENDING) {
                            for (BorrowRequestItem item : req.getBorrowRequestItems()) {
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

    @PostMapping("/{id}/rating")
    @ResponseBody
    public ResponseEntity<?> submitRating(@PathVariable("id") Long bookId,
                                          @RequestParam("rating") Integer rating,
                                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Bạn cần đăng nhập để đánh giá!"));
        }
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            bookReviewService.saveReview(bookId, user.getId(), rating);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cảm ơn bạn đã đánh giá truyện!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/comment")
    @ResponseBody
    public ResponseEntity<?> submitComment(@PathVariable("id") Long bookId,
                                           @RequestParam("content") String content,
                                           Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Bạn cần đăng nhập để bình luận!"));
        }
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            bookCommentService.addComment(bookId, user.getId(), content);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi bình luận!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/comments/{commentId}/hide")
    @ResponseBody
    public ResponseEntity<?> hideComment(@PathVariable("commentId") Long commentId) {
        try {
            bookCommentService.hideComment(commentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã ẩn bình luận"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/comments/{commentId}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteComment(@PathVariable("commentId") Long commentId,
                                           @RequestParam(value = "reason", required = false) String reason) {
        try {
            bookCommentService.deleteCommentByAdmin(commentId, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa bình luận"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/comments/{commentId}/edit")
    @ResponseBody
    public ResponseEntity<?> editCommentByUser(
            @PathVariable("commentId") Long commentId,
            @RequestParam("content") String content,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
            }
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            bookCommentService.editCommentByUser(commentId, user.getId(), content);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật bình luận!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/comments/{commentId}/user-delete")
    @ResponseBody
    public ResponseEntity<?> deleteCommentByUser(
            @PathVariable("commentId") Long commentId,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
            }
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            bookCommentService.deleteCommentByUser(commentId, user.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa bình luận!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/add")
    public String showAddForm(Model model) {
        return "redirect:/books";
    }

    // thêm mới truyện
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

    // chỉnh sửa truyện
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        return "redirect:/books";
    }

    // cập nhật truyện
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/update/{id}")
    public String updateBook(@PathVariable Long id, 
                             @Valid @ModelAttribute("editBookDTO") BookDTO bookDTO, 
                             BindingResult bindingResult, 
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(required = false) String query,
                             @RequestParam(required = false) Long category,
                             @RequestParam(required = false) String sortBy,
                             Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateListModel(model, query, category, page, sortBy, authentication);
            model.addAttribute("bookDTO", new BookDTO());
            model.addAttribute("showEditModal", true);
            model.addAttribute("editBookId", id);
            return "book/list";
        }
        try {
            bookService.updateBook(id, bookDTO);
            redirectAttributes.addFlashAttribute("message", "Cập nhật sách thành công");
        } catch (RuntimeException e) {
            populateListModel(model, query, category, page, sortBy, authentication);
            model.addAttribute("bookDTO", new BookDTO());
            model.addAttribute("showEditModal", true);
            model.addAttribute("editBookId", id);
            bindingResult.rejectValue("isbn", "error.editBookDTO", e.getMessage());
            return "book/list";
        }
        
        redirectAttributes.addAttribute("page", page);
        if (query != null && !query.isEmpty()) redirectAttributes.addAttribute("query", query);
        if (category != null) redirectAttributes.addAttribute("category", category);
        if (sortBy != null && !sortBy.isEmpty()) redirectAttributes.addAttribute("sortBy", sortBy);
        
        return "redirect:/books" + (query != null && !query.isEmpty() ? "/search" : "");
    }

    // Xóa truyện
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

    // Nhập thêm truyện
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/import/{id}")
    public String importBooks(@PathVariable Long id, @RequestParam("addedQuantity") int addedQuantity, RedirectAttributes redirectAttributes) {
        try {
            bookService.importBooks(id, addedQuantity);
            redirectAttributes.addFlashAttribute("message", "Nhập thêm truyện thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }

    // Check isbn
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
