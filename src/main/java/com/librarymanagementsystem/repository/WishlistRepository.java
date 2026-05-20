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

    // check wishlist đã tồn tại chưa
    Optional<Wishlist> findByUserAndBook(User user, Book book);

    // Xóa wishlist theo user và book
    void deleteByUserAndBook(User user, Book book);

    // count wishlist theo user
    long countByUser(User user);

    // danh sách wishlist theo userId
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId")
    List<Wishlist> findAllByUserId(@Param("userId") Long userId);
}
