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
import com.librarymanagementsystem.service.book.GeminiService;
import com.librarymanagementsystem.service.book.SystemSettingService;
import com.librarymanagementsystem.service.notification.NotificationService;
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
    private final NotificationService notificationService;
    private final GeminiService geminiService;
    private final SystemSettingService systemSettingService;

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

    private String applyManualFilter(String text) {
        List<SensitiveWord> sensitiveWords = sensitiveWordRepository.findAll();
        String filtered = text;
        for (SensitiveWord sw : sensitiveWords) {
            String word = sw.getWord();
            filtered = filtered.replaceAll("(?i)\\b" + word + "\\b", "****");
        }
        return filtered;
    }

    private String moderateContent(String content, BookComment comment) {
        if (systemSettingService.isAiModerationEnabled()) {
            try {
                String apiKey = systemSettingService.getAiApiKey();
                String action = systemSettingService.getAiModerationAction();

                GeminiService.ModerationResult result = geminiService.moderateText(content, apiKey);
                if (result.isSensitive()) {
                    if ("BLOCK".equalsIgnoreCase(action)) {
                        throw new RuntimeException("Bình luận chứa nội dung không phù hợp: " + result.getReason());
                    } else if ("HIDE".equalsIgnoreCase(action)) {
                        comment.setHidden(true);
                        return result.getCensoredText();
                    } else { // CENSOR
                        // Auto-learn new sensitive words!
                        for (String word : result.getSensitiveWordsFound()) {
                            if (word != null && !word.trim().isEmpty()) {
                                String cleanWord = word.trim().toLowerCase();
                                if (!sensitiveWordRepository.existsByWordIgnoreCase(cleanWord)) {
                                    sensitiveWordRepository.save(new SensitiveWord(null, cleanWord));
                                }
                            }
                        }
                        return result.getCensoredText();
                    }
                } else {
                    return result.getCensoredText();
                }
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Bình luận chứa nội dung không phù hợp")) {
                    throw e;
                }
                // Fallback to manual filter if AI fails
                return applyManualFilter(content);
            }
        } else {
            return applyManualFilter(content);
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

        BookComment comment = new BookComment();
        comment.setBook(book);
        comment.setUser(user);
        comment.setHidden(false);
        comment.setCreatedDate(LocalDateTime.now());

        String filteredContent = moderateContent(content, comment);
        comment.setContent(filteredContent);

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

        String filteredContent = moderateContent(newContent, comment);
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

    @Override
    public void deleteCommentByAdmin(Long commentId, String reason) {
        BookComment comment = bookCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));
        User user = comment.getUser();
        String bookTitle = comment.getBook().getTitle();

        bookCommentRepository.deleteById(commentId);

        if (reason != null && !reason.trim().isEmpty()) {
            String title = "Bình luận bị xóa";
            String content = "Bình luận của bạn tại truyện '" + bookTitle + "' đã bị quản trị viên xóa với lý do: " + reason;
            notificationService.sendNotification(user, title, content, "/books/" + comment.getBook().getId());
        }
    }
}
