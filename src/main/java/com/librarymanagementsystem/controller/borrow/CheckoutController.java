package com.librarymanagementsystem.controller.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.status.DeliveryMethod;
import com.librarymanagementsystem.model.cart.Cart;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.service.cart.CartService;
import com.librarymanagementsystem.service.shipping.ShippingService;
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
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CartService cartService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private ShippingService shippingService;

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
    public String viewCheckout(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        // Yêu cầu cập nhật địa chỉ trước khi thanh toán
        if (currentUser.getAddress() == null || currentUser.getAddress().trim().isEmpty()) {
            return "redirect:/user/profile?error=address_required";
        }

        Cart cart = cartService.getCartByUserId(currentUser.getId());
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("user", currentUser);
        
        int totalQuantity = cart.getItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
        
        double totalDeposit = cart.getItems().stream()
                .mapToDouble(item -> (item.getBook().getDepositPrice() != null ? item.getBook().getDepositPrice() : 0.0) * item.getQuantity())
                .sum();
        model.addAttribute("totalDeposit", totalDeposit);

        double defaultShippingFee = 0.0;
        if (currentUser.getAddress() != null && !currentUser.getAddress().isEmpty()) {
            double distance = shippingService.calculateDistance(currentUser.getAddress());
            if (distance >= 0) {
                defaultShippingFee = shippingService.calculateShippingFee(distance, totalQuantity);
            }
        }
        model.addAttribute("shippingFee", defaultShippingFee);
        model.addAttribute("totalAmount", totalDeposit + defaultShippingFee);

        return "user/checkout";
    }

    @PostMapping("/calculate-fee")
    @ResponseBody
    public ResponseEntity<?> calculateShippingFee(@RequestParam String address) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Cart cart = cartService.getCartByUserId(currentUser.getId());
            int totalQuantity = cart.getItems().stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();

            double distance = shippingService.calculateDistance(address);
            if (distance < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể tính toán khoảng cách"));
            }
            double fee = shippingService.calculateShippingFee(distance, totalQuantity);
            return ResponseEntity.ok(Map.of(
                    "distance", distance,
                    "fee", fee
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public String processCheckout(
            @RequestParam String deliveryMethod,
            @RequestParam(required = false) String shippingAddress,
            @RequestParam(required = false) String note,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        try {
            DeliveryMethod method = DeliveryMethod.valueOf(deliveryMethod);
            BorrowRequest request = borrowService.checkout(currentUser.getId(), method, shippingAddress, note);
            return "redirect:/borrow/history?success=checkout";
        } catch (Exception e) {
            try {
                String encodedError = java.net.URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống", "UTF-8");
                return "redirect:/checkout?error=" + encodedError;
            } catch (Exception ex) {
                return "redirect:/checkout?error=error_processing_checkout";
            }
        }
    }
}
