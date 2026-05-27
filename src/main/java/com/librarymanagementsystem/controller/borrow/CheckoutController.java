package com.librarymanagementsystem.controller.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.status.DeliveryMethod;
import com.librarymanagementsystem.model.cart.Cart;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.service.cart.CartService;
import com.librarymanagementsystem.service.shipping.ShippingService;
import com.librarymanagementsystem.service.user.UserService;
import com.librarymanagementsystem.service.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
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

    @Autowired
    private VNPayService vnPayService;

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
            return "redirect:/cart?error=address_required";
        }

        Map<String, Object> summary = cartService.getCartSummary(currentUser.getId());
        Cart cart = (Cart) summary.get("cart");
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAllAttributes(summary);
        model.addAttribute("user", currentUser);

        return "cart/checkout";
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
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) return "redirect:/login";

        try {
            DeliveryMethod method = DeliveryMethod.valueOf(deliveryMethod);
            
            // 1. Kiểm tra tồn kho của các sách trong giỏ hàng trước khi thanh toán
            Cart cart = cartService.getCartByUserId(currentUser.getId());
            if (cart.getItems().isEmpty()) {
                throw new RuntimeException("Giỏ hàng đang trống");
            }
            for (com.librarymanagementsystem.model.cart.CartItem item : cart.getItems()) {
                if (item.getBook().getQuantity() < item.getQuantity()) {
                    throw new RuntimeException("Sách " + item.getBook().getTitle() + " không đủ số lượng trong kho");
                }
            }

            // 2. Tính toán tổng số tiền cần thanh toán
            Map<String, Object> summary = cartService.getCartSummary(currentUser.getId());
            BigDecimal totalDeposit = (BigDecimal) summary.get("totalDeposit");
            BigDecimal shippingFee = BigDecimal.ZERO;
            if (method == DeliveryMethod.SHIPPING) {
                int totalQuantity = cart.getItems().stream()
                        .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                        .sum();
                double distance = shippingService.calculateDistance(shippingAddress);
                shippingFee = BigDecimal.valueOf(shippingService.calculateShippingFee(distance, totalQuantity));
            }
            BigDecimal totalAmount = totalDeposit.add(shippingFee);

            // 3. Lưu thông tin đơn mượn tạm thời vào Session để dùng lại sau khi VNPAY callback thành công
            session.setAttribute("checkout_deliveryMethod", deliveryMethod);
            session.setAttribute("checkout_shippingAddress", shippingAddress);
            session.setAttribute("checkout_note", note);
            session.setAttribute("checkout_userId", currentUser.getId());

            // 4. Tạo URL thanh toán VNPAY
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String orderInfo = "Thanh toan don muon sach " + currentUser.getId();
            String paymentUrl = vnPayService.createOrder(totalAmount.intValue(), orderInfo, baseUrl, "/checkout/vnpay-return");

            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            try {
                String encodedError = URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống", "UTF-8");
                return "redirect:/checkout?error=" + encodedError;
            } catch (Exception ex) {
                return "redirect:/checkout?error=error_processing_checkout";
            }
        }
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        boolean isValidSignature = vnPayService.verifySignature(request);
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");

        // Lấy thông tin đã lưu trong session
        Long userId = (Long) session.getAttribute("checkout_userId");
        String deliveryMethod = (String) session.getAttribute("checkout_deliveryMethod");
        String shippingAddress = (String) session.getAttribute("checkout_shippingAddress");
        String note = (String) session.getAttribute("checkout_note");

        // Dọn dẹp session
        session.removeAttribute("checkout_userId");
        session.removeAttribute("checkout_deliveryMethod");
        session.removeAttribute("checkout_shippingAddress");
        session.removeAttribute("checkout_note");

        if (userId == null || deliveryMethod == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên làm việc hết hạn hoặc không hợp lệ.");
            return "redirect:/cart";
        }

        if (isValidSignature && "00".equals(vnp_ResponseCode)) {
            try {
                DeliveryMethod method = DeliveryMethod.valueOf(deliveryMethod);
                borrowService.checkout(userId, method, shippingAddress, note);
                
                redirectAttributes.addFlashAttribute("message", "Thanh toán thành công qua VNPAY! Đơn mượn truyện đã được tạo.");
                return "redirect:/borrow/history?success=checkout";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu đơn mượn: " + e.getMessage());
                return "redirect:/checkout";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Thanh toán VNPAY không thành công hoặc giao dịch bị hủy.");
            return "redirect:/checkout";
        }
    }
}
