package com.librarymanagementsystem.repository;

import com.librarymanagementsystem.model.book.BookReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    List<BookReview> findByBookIdOrderByCreatedDateDesc(Long bookId);

    Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId);

    @Query("SELECT AVG(br.rating) FROM BookReview br WHERE br.book.id = :bookId")
    Double getAverageRatingForBook(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(br) FROM BookReview br WHERE br.book.id = :bookId")
    Long countReviewsForBook(@Param("bookId") Long bookId);
}
