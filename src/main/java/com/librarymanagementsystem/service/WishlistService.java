package com.librarymanagementsystem.service;

import com.librarymanagementsystem.model.book.Wishlist;

import java.util.List;
import java.util.Optional;

public interface WishlistService {

    boolean addToWishlist(Long userId, Long bookId);

    void removeFromWishlist(Long userId, Long bookId);

    boolean isInWishlist(Long userId, Long bookId);

    List<Wishlist> getUserWishlist(Long userId);

    Optional<Wishlist> getWishlistDetail(Long wishlistId);

    void clearUserWishlist(Long userId);

    long countUserWishlist(Long userId);
}
