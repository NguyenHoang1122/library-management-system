package com.librarymanagementsystem.repository;

import com.librarymanagementsystem.model.book.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(String title, String authorName);
    List<Book> findByAuthorId(Long authorId);
    List<Book> findByCategoriesId(Long categoryId);
    boolean existsByIsbn(String isbn);

    // 5 sách mới nhất
    List<Book> findTop5ByOrderByCreatedDateDesc();

    // 5 sách theo tên thể loại
    @Query("SELECT b FROM Book b JOIN b.categories c WHERE c.categoryName = :categoryName")
    List<Book> findTop5ByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    // 5 sách hot nhất (mượn nhiều nhất)
    @Query("SELECT bi.book FROM BorrowItem bi GROUP BY bi.book.id ORDER BY COUNT(bi.id) DESC")
    List<Book> findTop5HotBooks(Pageable pageable);
}
