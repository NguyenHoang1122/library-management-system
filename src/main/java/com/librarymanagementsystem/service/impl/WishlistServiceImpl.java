package com.librarymanagementsystem.service.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.Wishlist;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.BookRepository;
import com.librarymanagementsystem.repository.UserRepository;
import com.librarymanagementsystem.repository.WishlistRepository;
import com.librarymanagementsystem.service.WishlistService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    public boolean addToWishlist(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        // Check if already in wishlist - if yes, remove it (toggle)
        Optional<Wishlist> existing = wishlistRepository.findByUserAndBook(user, book);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false; // Removed
        }

        // Add to wishlist
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setBook(book);
        wishlistRepository.save(wishlist);
        return true; // Added
    }

    @Override
    public void removeFromWishlist(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        wishlistRepository.deleteByUserAndBook(user, book);
    }

    @Override
    public boolean isInWishlist(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        return wishlistRepository.findByUserAndBook(user, book).isPresent();
    }

    @Override
    public List<Wishlist> getUserWishlist(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        return wishlistRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Wishlist> getWishlistDetail(Long wishlistId) {
        return wishlistRepository.findById(wishlistId);
    }

    @Override
    public void clearUserWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<Wishlist> wishlists = wishlistRepository.findByUser(user);
        wishlistRepository.deleteAll(wishlists);
    }

    @Override
    public long countUserWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        return wishlistRepository.countByUser(user);
    }
}
