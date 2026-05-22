package com.librarymanagementsystem.repository.book;

import com.librarymanagementsystem.model.book.BookComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCommentRepository extends JpaRepository<BookComment, Long> {
    List<BookComment> findByBookIdOrderByCreatedDateDesc(Long bookId);
    Page<BookComment> findByBookIdOrderByCreatedDateDesc(Long bookId, Pageable pageable);
    
    @Query("SELECT c FROM BookComment c WHERE c.book.id = :bookId AND (c.isHidden = false OR c.user.id = :userId) ORDER BY c.createdDate DESC")
    Page<BookComment> findVisibleCommentsForUser(@Param("bookId") Long bookId, @Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT c FROM BookComment c WHERE c.book.id = :bookId AND c.isHidden = false ORDER BY c.createdDate DESC")
    Page<BookComment> findVisibleComments(@Param("bookId") Long bookId, Pageable pageable);
}
