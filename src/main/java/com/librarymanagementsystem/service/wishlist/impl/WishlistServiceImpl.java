package com.librarymanagementsystem.service.wishlist.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.Wishlist;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.book.BookRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.repository.wishlist.WishlistRepository;
import com.librarymanagementsystem.service.wishlist.WishlistService;
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

    // Thêm hoặc xóa truyện khỏi wishlist
    @Override
    public boolean addToWishlist(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        // Kiểm tra nếu truyện đã tồn tại trong wishlist, nếu có thì xóa đi
        Optional<Wishlist> existing = wishlistRepository.findByUserAndBook(user, book);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false;
        }

        // Add to wishlist
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setBook(book);
        wishlistRepository.save(wishlist);
        return true;
    }

    // Xóa truyện khỏi wishlist của user
    @Override
    public void removeFromWishlist(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        wishlistRepository.deleteByUserAndBook(user, book);
    }

    // Check truyện có nằm trong wishlist của user không
    @Override
    public boolean isInWishlist(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Sách không tồn tại"));

        return wishlistRepository.findByUserAndBook(user, book).isPresent();
    }

    //danh sách wishlist của user
    @Override
    public List<Wishlist> getUserWishlist(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        return wishlistRepository.findAllByUserId(userId);
    }

    // Lấy chi tiết thông tin của một bản ghi wishlist cụ thể qua ID
    @Override
    public Optional<Wishlist> getWishlistDetail(Long wishlistId) {
        return wishlistRepository.findById(wishlistId);
    }

    // Xóa sạch toàn bộ các truyện trong wishlist của người dùng
    @Override
    public void clearUserWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<Wishlist> wishlists = wishlistRepository.findAllByUserId(userId);
        wishlistRepository.deleteAll(wishlists);
    }

    // Đếm tổng số lượng truyện mà người dùng đã đưa vào danh sách yêu thích
    @Override
    public long countUserWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        return wishlistRepository.countByUser(user);
    }
}
