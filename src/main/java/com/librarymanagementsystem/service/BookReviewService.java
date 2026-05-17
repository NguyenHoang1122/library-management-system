package com.librarymanagementsystem.service;

import com.librarymanagementsystem.model.book.BookReview;

import java.util.List;
import java.util.Optional;

public interface BookReviewService {
    List<BookReview> getReviewsByBookId(Long bookId);
    Optional<BookReview> getReviewByBookAndUser(Long bookId, Long userId);
    BookReview saveReview(Long bookId, Long userId, Integer rating, String comment);
    boolean hasUserRentedBook(Long userId, Long bookId);
    Double getAverageRatingForBook(Long bookId);
    Long countReviewsForBook(Long bookId);
}
