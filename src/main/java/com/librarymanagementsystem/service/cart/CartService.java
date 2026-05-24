package com.librarymanagementsystem.service.cart;

import com.librarymanagementsystem.model.cart.Cart;

public interface CartService {
    Cart getCartByUserId(Long userId);
    Cart addToCart(Long userId, Long bookId, Integer quantity);
    Cart updateCartItem(Long userId, Long bookId, Integer quantity);
    void removeFromCart(Long userId, Long bookId);
    void clearCart(Long userId);
    Integer getCartItemCount(Long userId);
}
