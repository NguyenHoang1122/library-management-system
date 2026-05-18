package com.librarymanagementsystem.service.book.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.BookReview;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.BookRepository;
import com.librarymanagementsystem.repository.BookReviewRepository;
import com.librarymanagementsystem.repository.UserRepository;
import com.librarymanagementsystem.repository.borrow.BorrowTransactionRepository;
import com.librarymanagementsystem.service.book.BookReviewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class BookReviewServiceImpl implements BookReviewService {

    private final BookReviewRepository bookReviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowTransactionRepository borrowTransactionRepository;

    @Override
    public List<BookReview> getReviewsByBookId(Long bookId) {
        return bookReviewRepository.findByBookIdOrderByCreatedDateDesc(bookId);
    }

    @Override
    public Optional<BookReview> getReviewByBookAndUser(Long bookId, Long userId) {
        return bookReviewRepository.findByBookIdAndUserId(bookId, userId);
    }

    @Override
    public BookReview saveReview(Long bookId, Long userId, Integer rating, String comment) {
        if (!hasUserRentedBook(userId, bookId)) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá và bình luận sau khi đã thuê truyện này!");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Đánh giá phải từ 1 đến 5 sao!");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Truyện không tồn tại"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        BookReview review = bookReviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElse(new BookReview());

        review.setBook(book);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedDate(LocalDateTime.now());

        return bookReviewRepository.save(review);
    }

    @Override
    public boolean hasUserRentedBook(Long userId, Long bookId) {
        return borrowTransactionRepository.hasUserRentedBook(userId, bookId);
    }

    @Override
    public Double getAverageRatingForBook(Long bookId) {
        Double avg = bookReviewRepository.getAverageRatingForBook(bookId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public Long countReviewsForBook(Long bookId) {
        return bookReviewRepository.countReviewsForBook(bookId);
    }
}
