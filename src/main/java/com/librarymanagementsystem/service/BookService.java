package com.librarymanagementsystem.service;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.dto.BookDTO;

import java.util.List;
import java.util.Optional;

public interface BookService {
    List<Book> getAllBooks();
    Optional<Book> getBookById(Long id);
    Book saveBook(BookDTO bookDTO);
    Book updateBook(Long id, BookDTO bookDTO);
    void deleteBook(Long id);
    List<Book> searchBooks(String query);
    List<Book> getBooksByCategory(Long categoryId);
    List<Book> getBooksByAuthor(Long authorId);
    List<Book> getNewestBooks();
    List<Book> getHotBooks();
    List<Book> getBooksByCategoryName(String categoryName);
    boolean isBookBorrowedByUser(Long bookId, Long userId);
}
