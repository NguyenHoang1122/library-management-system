package com.librarymanagementsystem.service.wishlist;

import com.librarymanagementsystem.model.book.Wishlist;

import java.util.List;
import java.util.Optional;

public interface WishlistService {

    // Thêm truyện vào danh sách yêu thích
    boolean addToWishlist(Long userId, Long bookId);

    // Xóa truyện cụ thể ra khỏi danh sách yêu thích
    void removeFromWishlist(Long userId, Long bookId);

    // Kiểm tra truyện đã yêu thích chưa
    boolean isInWishlist(Long userId, Long bookId);

    // list yêu thích của user
    List<Wishlist> getUserWishlist(Long userId);

    // Lấy chi tiết thông tin của một bản ghi trong danh sách yêu thích
    Optional<Wishlist> getWishlistDetail(Long wishlistId);

    // Xóa toàn bộ truyện trong yêu thích
    void clearUserWishlist(Long userId);

    // count truyện trong yêu thích
    long countUserWishlist(Long userId);
}
