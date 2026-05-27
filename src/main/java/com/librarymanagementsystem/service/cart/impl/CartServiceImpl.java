package com.librarymanagementsystem.service.cart.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.cart.Cart;
import com.librarymanagementsystem.model.cart.CartItem;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.book.BookRepository;
import com.librarymanagementsystem.repository.cart.CartItemRepository;
import com.librarymanagementsystem.repository.cart.CartRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.service.cart.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private com.librarymanagementsystem.service.shipping.ShippingService shippingService;

    @Override
    @Transactional
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    @Override
    @Transactional
    public Cart addToCart(Long userId, Long bookId, Integer quantity) {
        Cart cart = getCartByUserId(userId);
        
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId);
        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Book not found"));
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setBook(book);
            newItem.setQuantity(quantity);
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }
        
        return cart;
    }

    @Override
    @Transactional
    public Cart updateCartItem(Long userId, Long bookId, Integer quantity) {
        Cart cart = getCartByUserId(userId);
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId);
        
        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            if (quantity <= 0) {
                cartItemRepository.delete(item);
                cart.getItems().remove(item);
            } else {
                item.setQuantity(quantity);
                cartItemRepository.save(item);
            }
        }
        return cart;
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long bookId) {
        Cart cart = getCartByUserId(userId);
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId);
        existingItemOpt.ifPresent(cartItem -> {
            cartItemRepository.delete(cartItem);
            cart.getItems().remove(cartItem);
        });
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    public Integer getCartItemCount(Long userId) {
        Cart cart = getCartByUserId(userId);
        return cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> getCartSummary(Long userId) {
        Cart cart = getCartByUserId(userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        int totalQuantity = cart.getItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        
        double totalDeposit = cart.getItems().stream()
                .mapToDouble(item -> (item.getBook().getDepositPrice() != null ? item.getBook().getDepositPrice() : 0.0) * item.getQuantity())
                .sum();
        
        double shippingFee = 0.0;
        if (user.getAddress() != null && !user.getAddress().isEmpty()) {
             double distance = shippingService.calculateDistance(user.getAddress());
             if (distance >= 0) {
                 shippingFee = shippingService.calculateShippingFee(distance, totalQuantity);
             }
        }
        
        double totalAmount = totalDeposit + shippingFee;
        
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("cart", cart);
        summary.put("totalQuantity", totalQuantity);
        summary.put("totalDeposit", totalDeposit);
        summary.put("shippingFee", shippingFee);
        summary.put("totalAmount", totalAmount);
        return summary;
    }
}
