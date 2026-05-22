package com.librarymanagementsystem.controller.wishlist;

import com.librarymanagementsystem.model.book.Wishlist;
import com.librarymanagementsystem.model.borrow.BorrowRequestItem;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.repository.borrow.BorrowTransactionRepository;
import com.librarymanagementsystem.service.user.UserService;
import com.librarymanagementsystem.service.wishlist.WishlistService;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
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
import java.util.ArrayList;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WishlistController {
    private final WishlistService wishlistService;
    private final UserService userService;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final BorrowService borrowService;

    @GetMapping
    public String getMyWishlist(Model model, Authentication authentication) {
        String userName = authentication.getName();
        User user = userService.findByUserName(userName).orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Long userId = user.getId();

        List<Wishlist> wishlists = wishlistService.getUserWishlist(userId);
        model.addAttribute("wishlists", wishlists);
        model.addAttribute("totalWishlists", wishlists.size());

        // Lấy danh sách truyện đang được mượn
        List<Long> borrowedBookIds = borrowTransactionRepository
                .findBorrowedBookIdsByUser(userId);
        model.addAttribute("borrowedBookIds", borrowedBookIds);

        // Lấy danh sách truyện đang chờ duyệt mượn
        List<Long> pendingBookIds = new ArrayList<>();
        List<BorrowRequest> userRequests = borrowService.getUserBorrowRequests(userId);
        if (userRequests != null) {
            for (BorrowRequest req : userRequests) {
                if (req.getRequestStatus() == RequestStatus.PENDING) {
                    for (BorrowRequestItem item : req.getBorrowRequestItems()) {
                        pendingBookIds.add(item.getBook().getId());
                    }
                }
            }
        }
        model.addAttribute("pendingBookIds", pendingBookIds);

        return "wishlist/my-wishlist";
    }

    //Thêm hoặc xóa nhanh một cuốn truyện khỏi danh sách yêu thích
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

    //Kiểm tra xem một cuốn truyện cụ thể đã có trong danh sách yêu thích của người dùng hay chưa
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

    // Xóa một truyện khỏi danh sách yêu thích
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

    // Xóa toàn bộ danh sách truyện yêu thích
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
