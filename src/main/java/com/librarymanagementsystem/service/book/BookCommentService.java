package com.librarymanagementsystem.service.book;

import com.librarymanagementsystem.model.book.BookComment;
import com.librarymanagementsystem.model.user.User;

import java.util.List;

import org.springframework.data.domain.Page;

public interface BookCommentService {
    Page<BookComment> getCommentsForBook(Long bookId, boolean isAdminOrLibrarian, User currentUser, int page, int size);
    BookComment addComment(Long bookId, Long userId, String content);
    void hideComment(Long commentId);
    void deleteComment(Long commentId);
    void editCommentByUser(Long commentId, Long userId, String newContent);
    void deleteCommentByUser(Long commentId, Long userId);
    void deleteCommentByAdmin(Long commentId, String reason);
}
