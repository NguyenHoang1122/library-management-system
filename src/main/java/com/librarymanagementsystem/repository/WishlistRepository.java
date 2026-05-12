package com.librarymanagementsystem.repository;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.Wishlist;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // Kiểm tra sách có trong wishlist không
    Optional<Wishlist> findByUserAndBook(User user, Book book);

    // Lấy danh sách wishlist của user
    List<Wishlist> findByUser(User user);

    // Xóa sách khỏi wishlist
    void deleteByUserAndBook(User user, Book book);

    // Count wishlist items của user
    long countByUser(User user);

    // Query tùy chỉnh
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId")
    List<Wishlist> findAllByUserId(@Param("userId") Long userId);
}
