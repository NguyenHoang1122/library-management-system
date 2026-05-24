package com.librarymanagementsystem.service.book.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.BookComment;
import com.librarymanagementsystem.model.book.SensitiveWord;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.book.BookCommentRepository;
import com.librarymanagementsystem.repository.book.BookRepository;
import com.librarymanagementsystem.repository.book.SensitiveWordRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.service.book.BookCommentService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
@RequiredArgsConstructor
public class BookCommentServiceImpl implements BookCommentService {

    private final BookCommentRepository bookCommentRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final SensitiveWordRepository sensitiveWordRepository;

    @PostConstruct
    public void initSensitiveWords() {
        if (sensitiveWordRepository.count() == 0) {
            List<String> defaultWords = List.of("ngu", "dm", "vl", "chó", "fuck");
            for (String word : defaultWords) {
                sensitiveWordRepository.save(new SensitiveWord(null, word));
            }
        }
    }

    @Override
    public Page<BookComment> getCommentsForBook(Long bookId, boolean isAdminOrLibrarian, User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (isAdminOrLibrarian) {
            return bookCommentRepository.findByBookIdOrderByCreatedDateDesc(bookId, pageable);
        } else {
            if (currentUser != null) {
                return bookCommentRepository.findVisibleCommentsForUser(bookId, currentUser.getId(), pageable);
            } else {
                return bookCommentRepository.findVisibleComments(bookId, pageable);
            }
        }
    }

    @Override
    public BookComment addComment(Long bookId, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung bình luận không được để trống.");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Truyện không tồn tại"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Filter sensitive words
        List<SensitiveWord> sensitiveWords = sensitiveWordRepository.findAll();
        String filteredContent = content;
        for (SensitiveWord sw : sensitiveWords) {
            String word = sw.getWord();
            // Case insensitive replacement using regex
            filteredContent = filteredContent.replaceAll("(?i)\\b" + word + "\\b", "****");
        }

        BookComment comment = new BookComment();
        comment.setBook(book);
        comment.setUser(user);
        comment.setContent(filteredContent);
        comment.setHidden(false);
        comment.setCreatedDate(LocalDateTime.now());

        return bookCommentRepository.save(comment);
    }

    @Override
    public void hideComment(Long commentId) {
        BookComment comment = bookCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));
        comment.setHidden(true);
        bookCommentRepository.save(comment);
    }

    @Override
    public void deleteComment(Long commentId) {
        bookCommentRepository.deleteById(commentId);
    }

    @Override
    public void editCommentByUser(Long commentId, Long userId, String newContent) {
        BookComment comment = bookCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));
        
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bình luận này.");
        }

        if (comment.getCreatedDate().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa bình luận trong vòng 24 giờ sau khi đăng.");
        }

        if (newContent == null || newContent.trim().isEmpty()) {
            throw new RuntimeException("Nội dung bình luận không được để trống.");
        }

        // Lọc từ nhạy cảm
        List<SensitiveWord> sensitiveWords = sensitiveWordRepository.findAll();
        String filteredContent = newContent;
        for (SensitiveWord sw : sensitiveWords) {
            String word = sw.getWord();
            filteredContent = filteredContent.replaceAll("(?i)\\b" + word + "\\b", "****");
        }

        comment.setContent(filteredContent);
        comment.setEdited(true);
        bookCommentRepository.save(comment);
    }

    @Override
    public void deleteCommentByUser(Long commentId, Long userId) {
        BookComment comment = bookCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));
        
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này.");
        }

        if (comment.getCreatedDate().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new RuntimeException("Chỉ có thể xóa bình luận trong vòng 24 giờ sau khi đăng.");
        }

        bookCommentRepository.deleteById(commentId);
    }
}
