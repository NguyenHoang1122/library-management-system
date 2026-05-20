package com.librarymanagementsystem.service.book.impl;

import com.librarymanagementsystem.model.book.Book;
import org.springframework.data.domain.PageRequest;
import com.librarymanagementsystem.model.book.Category;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import com.librarymanagementsystem.model.user.Author;
import com.librarymanagementsystem.repository.AuthorRepository;
import com.librarymanagementsystem.repository.BookRepository;
import com.librarymanagementsystem.repository.CategoryRepository;
import com.librarymanagementsystem.repository.borrow.BorrowTransactionRepository;
import com.librarymanagementsystem.service.book.BookService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BorrowTransactionRepository borrowTransactionRepository;

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

        if (!book.getIsbn().equals(bookDTO.getIsbn()) && bookRepository.existsByIsbn(bookDTO.getIsbn())) {
            throw new RuntimeException("ISBN đã tồn tại");
        }


        // Cập nhật từ DTO
        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setIsbn(bookDTO.getIsbn());
        book.setPublishYear(bookDTO.getPublishYear());
        book.setQuantity(bookDTO.getQuantity());

        // Xử lý ảnh
        if (bookDTO.getImageFile() != null && !bookDTO.getImageFile().isEmpty()) {
            // Xóa ảnh cũ nếu có
            if (book.getImage() != null && !book.getImage().isEmpty()) {
                try {
                    String oldFileName = book.getImage().substring(book.getImage().lastIndexOf("/") + 1);
                    Path oldPath = Paths.get(upload + oldFileName);
                    Files.deleteIfExists(oldPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Upload ảnh mới
            String fileName = UUID.randomUUID().toString() + "_" + bookDTO.getImageFile().getOriginalFilename();
            Path path = Paths.get(upload + fileName);
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, bookDTO.getImageFile().getBytes());
                book.setImage("/uploads/" + fileName);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
            }
        }
        // Nếu không chọn ảnh mới, ảnh cũ sẽ được giữ lại (không thay đổi)
        if (bookDTO.getCategoryIds() != null && !bookDTO.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (Long categoryId : bookDTO.getCategoryIds()) {
                Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
                categories.add(category);
            }
            book.setCategories(categories);
        }

        // Cập nhật Author
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

        book.setUpdatedDate(LocalDateTime.now());
        return bookRepository.save(book);
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
    public List<Book> searchBooks(String query) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(query, query);
    }

    @Override
    public List<Book> getBooksByCategory(Long categoryId) {
        return bookRepository.findByCategoriesId(categoryId);
    }

    @Override
    public List<Book> getBooksByAuthor(Long authorId) {
        return bookRepository.findByAuthorId(authorId);
    }

    // Chuyển đổi dữ liệu từ BookDTO sang Book
    private Book mapDtoToEntity(BookDTO bookDTO) {
        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setIsbn(bookDTO.getIsbn());
        book.setPublishYear(bookDTO.getPublishYear());
        book.setQuantity(bookDTO.getQuantity());

        // Upload ảnh
        if (bookDTO.getImageFile() != null && !bookDTO.getImageFile().isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + bookDTO.getImageFile().getOriginalFilename();
            Path path = Paths.get(upload + fileName);
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, bookDTO.getImageFile().getBytes());
                book.setImage("/uploads/" + fileName); //
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

    // check có mượn truyện này k
    @Override
    public boolean isBookBorrowedByUser(Long bookId, Long userId) {
        return borrowTransactionRepository.isBookBorrowedByUser(userId, bookId);
    }

    // Lấy 5 cuốn truyện mới nhật
    @Override
    public List<Book> getNewestBooks() {
        return bookRepository.findTop5ByOrderByCreatedDateDesc();
    }

    // 5 cuốn hot nhất.
    @Override
    public List<Book> getHotBooks() {
        return bookRepository.findTop5HotBooks(PageRequest.of(0, 5));
    }

    // 5 cuốn theo thể loại.
    @Override
    public List<Book> getBooksByCategoryName(String categoryName) {
        return bookRepository.findTop5ByCategoryName(categoryName, PageRequest.of(0, 5));
    }

    // Check isbn
    @Override
    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }
}
