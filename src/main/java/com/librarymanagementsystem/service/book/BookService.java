package com.librarymanagementsystem.service.book;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookService {

    Page<Book> getAllBooks(Pageable pageable);

    Optional<Book> getBookById(Long id);

    Book saveBook(BookDTO bookDTO);

    Book updateBook(Long id, BookDTO bookDTO);

    void importBooks(Long bookId, int addedQuantity);

    void deleteBook(Long id);

    Page<Book> searchBooks(String query, Pageable pageable);

    Page<Book> getBooksByCategory(Long categoryId, Pageable pageable);

    Page<Book> getBooksByAuthor(Long authorId, Pageable pageable);

    //lay 5 cuốn truyện
    List<Book> getNewestBooks();
    List<Book> getHotBooks();
    List<Book> getBooksByCategoryName(String categoryName);

    // check user đã mượn cuốn sách này hay chưa
    boolean isBookBorrowedByUser(Long bookId, Long userId);

    // check isbn
    boolean existsByIsbn(String isbn);
}
