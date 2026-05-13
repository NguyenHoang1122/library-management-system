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
                            Model model,
                            Authentication authentication) {
        if (title != null && !title.isEmpty()) {
            model.addAttribute("books", bookService.searchBooks(title));
        } else {
            model.addAttribute("books", bookService.getAllBooks());
        }

        // Thêm thông tin mượn nếu user đã login
        if (authentication != null && authentication.isAuthenticated()) {
            String userName = authentication.getName();
            User user = userService.findByUserName(userName).orElse(null);
            if (user != null) {
                model.addAttribute("userId", user.getId());
            }
        }
        return "book/list";
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
        model.addAttribute("bookDTO", new BookDTO());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.getAllAuthors());
        return "book/book-form";
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
        Book book = bookService.getBookById(id).orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        BookDTO bookDTO = new BookDTO();
        // Map entity to DTO (bỏ qua imageFile nếu không edit ảnh)
        bookDTO.setId(id);
        bookDTO.setTitle(book.getTitle());
        bookDTO.setDescription(book.getDescription());
        bookDTO.setIsbn(book.getIsbn());
        bookDTO.setPublishYear(book.getPublishYear());
        bookDTO.setQuantity(book.getQuantity());
        bookDTO.setImage(book.getImage());

        if (book.getCategories() != null) bookDTO.setCategoryIds(book.getCategories().stream().map(Category::getId).collect(Collectors.toList()));
        if (book.getAuthor() != null) bookDTO.setAuthorName(book.getAuthor().getName());
        model.addAttribute("bookDTO", bookDTO);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.getAllAuthors());
        return "book/book-form";
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
