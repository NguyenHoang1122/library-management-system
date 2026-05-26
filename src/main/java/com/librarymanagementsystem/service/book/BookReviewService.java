package com.librarymanagementsystem.service.book;

import com.librarymanagementsystem.model.book.BookReview;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BookReviewService {
    // Toàn bộ đánh giá của 1 cuốn truyện
    List<BookReview> getReviewsByBookId(Long bookId);

    // đánh giá người dùng cho 1 cuốn truyện cụ thể
    Optional<BookReview> getReviewByBookAndUser(Long bookId, Long userId);

    BookReview saveReview(Long bookId, Long userId, Integer rating);

    // check user đã  mượn cuốn sách này hay chưa
    boolean hasUserRentedBook(Long userId, Long bookId);

    // Tính điểm TB
    Double getAverageRatingForBook(Long bookId);

    // count đánh giá
    Long countReviewsForBook(Long bookId);

    // Thống kê sao
    Map<Integer, Long> getRatingSummary(Long bookId);
}
