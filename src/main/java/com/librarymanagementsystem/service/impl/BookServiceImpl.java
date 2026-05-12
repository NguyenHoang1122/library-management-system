package com.librarymanagementsystem.service.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.Category;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import com.librarymanagementsystem.model.user.Author;
import com.librarymanagementsystem.repository.AuthorRepository;
import com.librarymanagementsystem.repository.BookRepository;
import com.librarymanagementsystem.repository.CategoryRepository;
import com.librarymanagementsystem.service.BookService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;

    @Value("${file.upload-dir}")
    private String upload;

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    public Book saveBook(BookDTO bookDTO) {
        if (bookRepository.existsByIsbn(bookDTO.getIsbn())) {
            throw new RuntimeException("ISBN đã tồn tại");
        }
        Book book = mapDtoToEntity(bookDTO);
        book.setCreatedDate(LocalDateTime.now());
        book.setUpdatedDate(LocalDateTime.now());
        return bookRepository.save(book);
    }

    @Override
    public Book updateBook(Long id, BookDTO bookDTO) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        Book updatedBook = mapDtoToEntity(bookDTO);
        updatedBook.setId(id);
        updatedBook.setCreatedDate(book.getCreatedDate());
        updatedBook.setUpdatedDate(LocalDateTime.now());
        return bookRepository.save(updatedBook);
    }

    @Override
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
        // Xóa file ảnh nếu có
        if (book.getImage() != null && !book.getImage().isEmpty()) {
            try {
                Path path = Paths.get(upload + book.getImage().substring(book.getImage().lastIndexOf("/") + 1));
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bookRepository.deleteById(id);
    }

    @Override
    public List<Book> searchBooks(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    public List<Book> getBooksByCategory(Long categoryId) {
        return bookRepository.findByCategoriesId(categoryId);
    }

    @Override
    public List<Book> getBooksByAuthor(Long authorId) {
        return bookRepository.findByAuthorId(authorId);
    }

    private Book mapDtoToEntity(BookDTO bookDTO) {
        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setIsbn(bookDTO.getIsbn());
        book.setPublishYear(bookDTO.getPublishYear());

        // Upload ảnh
        if (bookDTO.getImageFile() != null && !bookDTO.getImageFile().isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + bookDTO.getImageFile().getOriginalFilename();
            Path path = Paths.get(upload + fileName);
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, bookDTO.getImageFile().getBytes());
                book.setImage("/uploads/" + fileName); // Đường dẫn tương đối
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
            }
        }

        // Liên kết Category và Author
        if (bookDTO.getCategoryIds() != null && !bookDTO.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (Long categoryId : bookDTO.getCategoryIds()) {
                Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
                categories.add(category);
            }
            book.setCategories(categories);
        }
        if (bookDTO.getAuthorName() != null && !bookDTO.getAuthorName().trim().isEmpty()) {
            Optional<Author> existingAuthor = authorRepository.findByName(bookDTO.getAuthorName().trim());
            Author author;
            if (existingAuthor.isPresent()) {
                author = existingAuthor.get();
            } else {
                author = new Author();
                author.setName(bookDTO.getAuthorName().trim());
                author = authorRepository.save(author);
            }
            book.setAuthor(author);
        }

        return book;
    }
}
