package com.librarymanagementsystem.controller.book;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import com.librarymanagementsystem.service.AuthorService;
import com.librarymanagementsystem.service.BookService;
import com.librarymanagementsystem.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final AuthorService authorService;


    @GetMapping
    public String listBooks(@RequestParam(required = false) String title, Model model) {
        if (title != null && !title.isEmpty()) {
            model.addAttribute("books", bookService.searchBooks(title));
        } else {
            model.addAttribute("books", bookService.getAllBooks());
        }
        return "book/books"; // Template: user/books.html
    }

    @GetMapping("/{id}")
    public String viewBook(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getBookById(id).orElseThrow(() -> new RuntimeException("Sách không tồn tại")));
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
        bookDTO.setTitle(book.getTitle());
        bookDTO.setDescription(book.getDescription());
        bookDTO.setIsbn(book.getIsbn());
        bookDTO.setPublishYear(book.getPublishYear());
        bookDTO.setTotalCopies(book.getTotalCopies());
        if (book.getCategory() != null) bookDTO.setCategoryId(book.getCategory().getId());
        if (book.getAuthor() != null) bookDTO.setAuthorId(book.getAuthor().getId());
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
