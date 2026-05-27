package com.librarymanagementsystem.repository.book;

import com.librarymanagementsystem.model.book.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);

    // Tìm kiếm danh sách truyện
    Page<Book> findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(String title, String authorName, Pageable pageable);
    // Tìm danh sách truyện theo tác giả
    Page<Book> findByAuthorId(Long authorId, Pageable pageable);
    // Tìm danh sách truyện theo thể loại
    Page<Book> findByCategoriesId(Long categoryId, Pageable pageable);
    // Check isbn
    boolean existsByIsbn(String isbn);

    // Lấy 5 cuốn mới nhất
    List<Book> findTop5ByOrderByCreatedDateDesc();

    // 5 cuốn truyện theo thể loại
    @Query("SELECT b FROM Book b JOIN b.categories c WHERE c.categoryName = :categoryName")
    List<Book> findTop5ByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    // 5 cuốn truyện mượn nhiều nhất
    @Query("SELECT bi.bookCopy.book FROM BorrowItem bi GROUP BY bi.bookCopy.book.id ORDER BY COUNT(bi.id) DESC")
    List<Book> findTop5HotBooks(Pageable pageable);
}
