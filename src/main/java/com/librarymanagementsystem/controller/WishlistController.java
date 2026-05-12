package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.book.Wishlist;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.UserService;
import com.librarymanagementsystem.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class WishlistController {
    private final WishlistService wishlistService;
    private final UserService userService;

    /**     * Xem danh sách yêu thích của user     */
    @GetMapping
    public String showWishlist(Authentication authentication, Model model) {
        User user = userService.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<Wishlist> wishlists = wishlistService.getUserWishlist(user.getId());
        model.addAttribute("wishlists", wishlists);
        model.addAttribute("totalWishlists", wishlists.size());
        return "wishlist/my-wishlist";
    }

    /**     * Thêm hoặc xóa sách khỏi wishlist (AJAX)     */
    @PostMapping("/add/{bookId}")
    @ResponseBody
    public ResponseEntity<?> addToWishlist(@PathVariable Long bookId, Authentication authentication) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            boolean added = wishlistService.addToWishlist(user.getId(), bookId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("added", added);
            response.put("message", added ? "Đã thêm vào danh sách yêu thích" : "Đã xóa khỏi danh sách yêu thích");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**     * Kiểm tra sách có trong wishlist không (AJAX)     */
    @GetMapping("/check/{bookId}")
    @ResponseBody
    public ResponseEntity<?> checkWishlist(@PathVariable Long bookId, Authentication authentication) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            boolean inWishlist = wishlistService.isInWishlist(user.getId(), bookId);

            Map<String, Object> response = new HashMap<>();
            response.put("inWishlist", inWishlist);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**     * Xóa sách khỏi wishlist     */
    @PostMapping("/remove/{wishlistId}")
    public String removeFromWishlist(@PathVariable Long wishlistId,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Wishlist wishlist = wishlistService.getWishlistDetail(wishlistId)
                    .orElseThrow(() -> new RuntimeException("Wishlist không tồn tại"));

            User currentUser = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            // Kiểm tra quyền
            if (!wishlist.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa wishlist này");
                return "redirect:/wishlist";
            }

            wishlistService.removeFromWishlist(currentUser.getId(), wishlist.getBook().getId());
            redirectAttributes.addFlashAttribute("message", "Đã xóa khỏi danh sách yêu thích");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/wishlist";
    }

    /**     * Xóa tất cả wishlist của user     */
    @PostMapping("/clear-all")
    public String clearAllWishlist(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUserName(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            wishlistService.clearUserWishlist(user.getId());
            redirectAttributes.addFlashAttribute("message", "Đã xóa tất cả danh sách yêu thích");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/wishlist";
    }
}
