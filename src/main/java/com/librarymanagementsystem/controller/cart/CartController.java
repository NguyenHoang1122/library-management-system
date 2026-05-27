package com.librarymanagementsystem.controller.cart;

import com.librarymanagementsystem.model.cart.Cart;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.cart.CartService;
import com.librarymanagementsystem.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userService.findByUserName(auth.getName()).orElse(null);
        }
        return null;
    }

    @GetMapping
    public String viewCart(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        Map<String, Object> summary = cartService.getCartSummary(currentUser.getId());
        model.addAllAttributes(summary);

        return "cart/cart"; // Thymeleaf template location
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(@RequestParam Long bookId, @RequestParam(defaultValue = "1") Integer quantity) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
        }

        try {
            cartService.addToCart(currentUser.getId(), bookId, quantity);
            Integer count = cartService.getCartItemCount(currentUser.getId());
            return ResponseEntity.ok(Map.of("success", true, "cartCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<?> updateCart(@RequestParam Long bookId, @RequestParam Integer quantity) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
        }

        try {
            cartService.updateCartItem(currentUser.getId(), bookId, quantity);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam Long bookId) {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            cartService.removeFromCart(currentUser.getId(), bookId);
        }
        return "redirect:/cart";
    }
}
