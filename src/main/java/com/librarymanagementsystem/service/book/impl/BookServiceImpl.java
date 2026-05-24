package com.librarymanagementsystem.service.book.impl;

import com.librarymanagementsystem.model.book.Book;
import org.springframework.data.domain.PageRequest;
import com.librarymanagementsystem.model.book.Category;
import com.librarymanagementsystem.model.book.dto.BookDTO;
import com.librarymanagementsystem.model.user.Author;
import com.librarymanagementsystem.repository.book.author.AuthorRepository;
import com.librarymanagementsystem.repository.book.BookRepository;
import com.librarymanagementsystem.repository.book.BookCopyRepository;
import com.librarymanagementsystem.model.book.BookCopy;
import com.librarymanagementsystem.model.book.status.BookCopyStatus;
import com.librarymanagementsystem.model.book.BookImportHistory;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import com.librarymanagementsystem.repository.book.category.CategoryRepository;
import com.librarymanagementsystem.repository.borrow.BorrowTransactionRepository;
import com.librarymanagementsystem.repository.book.BookImportHistoryRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final BookImportHistoryRepository bookImportHistoryRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String upload;

    @Override
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
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
        Book savedBook = bookRepository.save(book);

        // Tạo book copies
        if (savedBook.getQuantity() != null && savedBook.getQuantity() > 0) {
            for (int i = 0; i < savedBook.getQuantity(); i++) {
                BookCopy copy = new BookCopy();
                copy.setBook(savedBook);
                copy.setBarcode(savedBook.getIsbn() + "-" + (i + 1));
                copy.setStatus(BookCopyStatus.AVAILABLE);
                copy.setCreatedDate(LocalDateTime.now());
                bookCopyRepository.save(copy);
            }
            
            // Deduct admin money and save history
            double totalPrice = savedBook.getQuantity() * (savedBook.getImportPrice() != null ? savedBook.getImportPrice() : 0.0);
            deductAdminWallet(totalPrice);
            saveImportHistory(savedBook, savedBook.getQuantity(), savedBook.getImportPrice(), totalPrice);
        }
        
        return savedBook;
    }

    private void deductAdminWallet(double amount) {
        if (amount <= 0) return;
        List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
        if (!admins.isEmpty()) {
            User admin = admins.get(0);
            double currentBalance = admin.getBalance() != null ? admin.getBalance() : 0.0;
            admin.setBalance(currentBalance - amount);
            userRepository.save(admin);
        }
    }

    private void saveImportHistory(Book book, int quantity, Double importPrice, double totalPrice) {
        BookImportHistory history = new BookImportHistory();
        history.setBook(book);
        history.setImportQuantity(quantity);
        history.setImportPrice(importPrice);
        history.setTotalPrice(totalPrice);
        history.setImportDate(LocalDateTime.now());
        bookImportHistoryRepository.save(history);
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
        
        // Xử lý đồng bộ số lượng BookCopy
        int currentQuantity = book.getQuantity() != null ? book.getQuantity() : 0;
        int newQuantity = bookDTO.getQuantity() != null ? bookDTO.getQuantity() : 0;
        
        if (newQuantity > currentQuantity) {
            int diff = newQuantity - currentQuantity;
            for (int i = 0; i < diff; i++) {
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copy.setBarcode(book.getIsbn() + "-" + System.currentTimeMillis() + "-" + i);
                copy.setStatus(BookCopyStatus.AVAILABLE);
                copy.setCreatedDate(LocalDateTime.now());
                bookCopyRepository.save(copy);
            }
            
            // Deduct admin money and save history for added books
            double importPrice = bookDTO.getImportPrice() != null ? bookDTO.getImportPrice() : 0.0;
            double totalPrice = diff * importPrice;
            deductAdminWallet(totalPrice);
            saveImportHistory(book, diff, importPrice, totalPrice);
        } else if (newQuantity < currentQuantity) {
            int diff = currentQuantity - newQuantity;
            List<BookCopy> availableCopies = bookCopyRepository.findByBookIdAndStatus(book.getId(), BookCopyStatus.AVAILABLE);
            if (availableCopies.size() < diff) {
                throw new RuntimeException("Không thể giảm số lượng truyện xuống " + newQuantity + " vì số truyện có sẵn trong kho không đủ để trừ (có thể do đang được mượn). Chỉ có thể giảm đi tối đa " + availableCopies.size() + " cuốn.");
            }
            // Xóa đi 'diff' cuốn AVAILABLE
            for (int i = 0; i < diff; i++) {
                bookCopyRepository.delete(availableCopies.get(i));
            }
        }
        book.setQuantity(newQuantity);
        
        book.setImportPrice(bookDTO.getImportPrice());
        book.setDepositPrice(bookDTO.getDepositPrice());

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
    @org.springframework.transaction.annotation.Transactional
    public void importBooks(Long bookId, int addedQuantity) {
        if (addedQuantity <= 0) {
            throw new RuntimeException("Số lượng nhập phải lớn hơn 0");
        }
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        // Tạo book copies
        for (int i = 0; i < addedQuantity; i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setBarcode(book.getIsbn() + "-" + System.currentTimeMillis() + "-" + i);
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copy.setCreatedDate(LocalDateTime.now());
            bookCopyRepository.save(copy);
        }

        // Cập nhật số lượng sách
        int currentQuantity = book.getQuantity() != null ? book.getQuantity() : 0;
        book.setQuantity(currentQuantity + addedQuantity);
        bookRepository.save(book);

        // Deduct admin money and save history
        double importPrice = book.getImportPrice() != null ? book.getImportPrice() : 0.0;
        double totalPrice = addedQuantity * importPrice;
        deductAdminWallet(totalPrice);
        saveImportHistory(book, addedQuantity, importPrice, totalPrice);
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
        
        // Xóa liên kết foreign key bảng book_categories trước khi xóa
        book.getCategories().clear();
        bookRepository.save(book);
        
        try {
            bookRepository.delete(book);
            bookRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Không thể xóa truyện này vì đã phát sinh giao dịch mượn/trả hoặc đang nằm trong giỏ hàng/yêu thích của người dùng.");
        }
    }

    @Override
    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(query, query, pageable);
    }

    @Override
    public Page<Book> getBooksByCategory(Long categoryId, Pageable pageable) {
        return bookRepository.findByCategoriesId(categoryId, pageable);
    }

    @Override
    public Page<Book> getBooksByAuthor(Long authorId, Pageable pageable) {
        return bookRepository.findByAuthorId(authorId, pageable);
    }

    // Chuyển đổi dữ liệu từ BookDTO sang Book
    private Book mapDtoToEntity(BookDTO bookDTO) {
        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setDescription(bookDTO.getDescription());
        book.setIsbn(bookDTO.getIsbn());
        book.setPublishYear(bookDTO.getPublishYear());
        book.setQuantity(bookDTO.getQuantity());
        book.setImportPrice(bookDTO.getImportPrice());
        book.setDepositPrice(bookDTO.getDepositPrice());

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
