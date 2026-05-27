package com.librarymanagementsystem.service.borrow.impl;

import com.librarymanagementsystem.model.book.Book;
import com.librarymanagementsystem.model.book.BookCopy;
import com.librarymanagementsystem.model.book.status.BookCopyStatus;
import com.librarymanagementsystem.model.borrow.*;
import com.librarymanagementsystem.model.borrow.dto.BorrowHistoryDTO;
import com.librarymanagementsystem.model.borrow.dto.CombinedHistoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.borrow.status.TransactionStatus;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import com.librarymanagementsystem.repository.book.BookRepository;
import com.librarymanagementsystem.repository.book.BookCopyRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.repository.borrow.*;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.service.cart.CartService;
import com.librarymanagementsystem.service.shipping.ShippingService;
import com.librarymanagementsystem.model.borrow.status.DeliveryMethod;
import com.librarymanagementsystem.model.cart.Cart;
import com.librarymanagementsystem.model.cart.CartItem;
import com.librarymanagementsystem.service.notification.NotificationService;
import com.librarymanagementsystem.service.user.AdminWalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final BorrowRequestItemRepository borrowRequestItemRepository;
    private final BorrowItemRepository borrowItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final NotificationService notificationService;
    private final ReturnRequestRepository returnRequestRepository;
    private final CartService cartService;
    private final ShippingService shippingService;
    private final AdminWalletService adminWalletService;

    private final Integer DEFAULT_BORROW_DAYS = 7;
    private final long DAILY_FINE = 7000; //phạt trả muộn

    // Yêu cầu mượn sách từ giỏ hàng (Checkout)
    @Override
    public BorrowRequest checkout(Long userId, DeliveryMethod deliveryMethod, String shippingAddress, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Cart cart = cartService.getCartByUserId(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống");
        }

        // Sắp xếp cart items theo bookId để tránh deadlock khi lock nhiều record
       List<CartItem> sortedCartItems = new ArrayList<>(cart.getItems());
        sortedCartItems.sort(Comparator.comparing(item -> item.getBook().getId()));

        // Tính tổng tiền cọc và acquire DB lock cho các sách trong giỏ
        BigDecimal totalDeposit = BigDecimal.ZERO;
        for (CartItem item : sortedCartItems) {
            Book lockedBook = bookRepository.findByIdWithLock(item.getBook().getId())
                    .orElseThrow(() -> new RuntimeException("Truyện không tồn tại"));
            
            if (lockedBook.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sách " + lockedBook.getTitle() + " không đủ số lượng trong kho");
            }
            item.setBook(lockedBook); // Cập nhật lại book trong item để dùng book đã lock ở bước sau

            BigDecimal dp = lockedBook.getDepositPrice() != null ? lockedBook.getDepositPrice() : BigDecimal.ZERO;
            totalDeposit = totalDeposit.add(dp.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Tính phí ship
        double distance = 0.0;
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (deliveryMethod == DeliveryMethod.SHIPPING) {
            int totalQuantity = cart.getItems().stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();
            distance = shippingService.calculateDistance(shippingAddress);
            shippingFee = BigDecimal.valueOf(shippingService.calculateShippingFee(distance, totalQuantity));
        }

        BigDecimal totalAmount = totalDeposit.add(shippingFee);
        // Loại bỏ thanh toán qua ví cá nhân, người dùng đã thanh toán VNPAY trực tiếp

        // Tạo yêu cầu mượn
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setUser(user);
        borrowRequest.setRequestDate(LocalDateTime.now());
        borrowRequest.setRequestStatus(RequestStatus.PENDING);
        borrowRequest.setNote(note);
        borrowRequest.setDeliveryMethod(deliveryMethod);
        borrowRequest.setShippingAddress(shippingAddress);
        borrowRequest.setDistance(distance);
        borrowRequest.setShippingFee(shippingFee);
        borrowRequest.setTotalDeposit(totalDeposit);
        borrowRequest = borrowRequestRepository.save(borrowRequest);

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        notificationService.sendNotification(user, "Thanh toán đơn mượn", 
            String.format("Đã nhận thanh toán %s đ bằng VNPAY cho đơn mượn truyện (Mã đơn: #%d).", 
                nf.format(totalAmount), borrowRequest.getId()), 
            "/borrow/history");

        // Ghi nhận doanh thu cho hệ thống (ví Admin)
        adminWalletService.logTransaction(totalAmount, "BORROW_INCOME", String.format("Nhận thanh toán đơn mượn trực tuyến #%d", borrowRequest.getId()), borrowRequest.getId());

        List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
        if (!admins.isEmpty()) {
            User admin = admins.get(0);
            notificationService.sendNotification(admin, "Nhận tiền thanh toán đơn mượn", 
                String.format("Nhận %s đ từ đơn mượn trực tuyến (Mã đơn: #%d) của độc giả %s. Cọc: %s đ, Phí ship: %s đ. Số dư hiện tại: %s đ.", 
                    nf.format(totalAmount), borrowRequest.getId(), user.getFullName() != null ? user.getFullName() : user.getUserName(), 
                    nf.format(totalDeposit), nf.format(shippingFee), nf.format(admin.getBalance())), 
                null);
        }

        // Chuyển items từ cart sang request
        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();
            int oldQuantity = book.getQuantity() != null ? book.getQuantity() : 0;
            book.setQuantity(oldQuantity - cartItem.getQuantity()); // Trừ số lượng ảo trước
            bookRepository.save(book);

            if (oldQuantity >= 20 && book.getQuantity() < 20) {
                notificationService.notifyLowStock(book);
            }

            BorrowRequestItem borrowRequestItem = new BorrowRequestItem();
            borrowRequestItem.setBorrowRequest(borrowRequest);
            borrowRequestItem.setBook(book);
            borrowRequestItem.setQuantity(cartItem.getQuantity());
            borrowRequestItemRepository.save(borrowRequestItem);
        }

        // Xóa giỏ hàng sau khi checkout
        cartService.clearCart(userId);

        notificationService.notifyReviewBorrowRequest(borrowRequest);

        return borrowRequest;
    }

    //yêu cầu mượn sách mới (tạo trực tiếp không qua giỏ hàng - giữ lại for compatibility)
    @Override
    public BorrowRequest createBorrowRequest(Long userId, Long bookId, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Truyện không tồn tại"));

        if (book.getQuantity() <= 0) {
            throw new RuntimeException("Truyện đã hết trong kho, không thể mượn lúc này");
        }

        // TRỪ SỐ LƯỢNG NGAY KHI TẠO YÊU CẦU ĐỂ TRÁNH TRANH CHẤP
        int oldQuantity = book.getQuantity() != null ? book.getQuantity() : 0;
        book.setQuantity(oldQuantity - 1);
        bookRepository.save(book);

        if (oldQuantity >= 20 && book.getQuantity() < 20) {
            notificationService.notifyLowStock(book);
        }

        // Tạo yêu cầu mượn
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setUser(user);
        borrowRequest.setRequestDate(LocalDateTime.now());
        borrowRequest.setRequestStatus(RequestStatus.PENDING);
        borrowRequest.setNote(note);
        borrowRequest = borrowRequestRepository.save(borrowRequest);

        // Thêm truyện vào yêu cầu
        BorrowRequestItem borrowRequestItem = new BorrowRequestItem();
        borrowRequestItem.setBorrowRequest(borrowRequest);
        borrowRequestItem.setBook(book);
        borrowRequestItem.setQuantity(1);
        borrowRequestItemRepository.save(borrowRequestItem);

        // Tạo thông báo cho thủ thư
        notificationService.notifyReviewBorrowRequest(borrowRequest);

        return borrowRequest;
    }

    @Override
    public List<Long> getBorrowedBookIdsByUser(Long userId) {
        return borrowTransactionRepository.findBorrowedBookIdsByUser(userId);
    }

    // Lấy danh sách các yêu cầu mượn truyện của user
    @Override
    public List<BorrowRequest> getUserBorrowRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return borrowRequestRepository.findByUserOrderByRequestDateDesc(user);
    }

    @Override
    public Page<BorrowRequest> getUserBorrowRequests(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return borrowRequestRepository.findByUser(user, pageable);
    }

    // thông tin của một yêu cầu mượn truyện cụ thể qua ID yêu cầu
    @Override
    public Optional<BorrowRequest> getBorrowRequestDetail(Long requestId) {
        return borrowRequestRepository.findById(requestId);
    }

    // Hủy bỏ mượn truyện
    public void cancelBorrowRequest(Long requestId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));

        if (borrowRequest.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy yêu cầu chưa duyệt");
        }

        borrowRequest.setRequestStatus(RequestStatus.CANCELLED);
        borrowRequestRepository.save(borrowRequest);

        // HOÀN LẠI SỐ LƯỢNG KHI HỦY
        List<BorrowRequestItem> items = borrowRequestItemRepository.findByBorrowRequest(borrowRequest);
        for (BorrowRequestItem item : items) {
            Book book = item.getBook();
            book.setQuantity(book.getQuantity() + item.getQuantity());
            bookRepository.save(book);
        }

        // TRỪ TIỀN KHỎI VÍ ADMIN VÌ HỦY ĐƠN (Thống kê chi phí hoàn tiền của thư viện)
        User user = borrowRequest.getUser();
        BigDecimal totalRefund = (borrowRequest.getTotalDeposit() != null ? borrowRequest.getTotalDeposit() : BigDecimal.ZERO)
                             .add(borrowRequest.getShippingFee() != null ? borrowRequest.getShippingFee() : BigDecimal.ZERO);
        
        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            adminWalletService.logTransaction(totalRefund.negate(), "REFUND_EXPENSE", String.format("Hoàn tiền đơn mượn bị hủy #%d", borrowRequest.getId()), borrowRequest.getId());

            List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
            if (!admins.isEmpty()) {
                User admin = admins.get(0);
                NumberFormat nfAdmin = NumberFormat.getInstance(new Locale("vi", "VN"));
                notificationService.sendNotification(admin, "Trừ tiền hủy đơn mượn", 
                    String.format("Trừ %s đ do đơn mượn (Mã đơn: #%d) của độc giả %s bị hủy. Số dư hiện tại: %s đ.", 
                        nfAdmin.format(totalRefund), borrowRequest.getId(), user.getFullName() != null ? user.getFullName() : user.getUserName(), 
                        nfAdmin.format(admin.getBalance())), 
                    null);
            }
        }

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String notifContent = String.format("Đơn mượn truyện (Mã đơn: #%d) của bạn đã được hủy thành công. Số tiền %s đ (Cọc + Ship) đã được làm thủ tục hoàn trả lại vào tài khoản ngân hàng của bạn.",
                borrowRequest.getId(), nf.format(totalRefund));
        
        notificationService.sendNotification(user, "Hủy đơn mượn thành công", notifContent, "/borrow/history");
    }

    // Phê duyệt yêu cầu mượn truyện (Chuyển sang trạng thái đang giao)
    @Override
    public void approveBorrowRequest(Long requestId, Long librarianId, Integer borrowDays) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Thủ thư không tồn tại"));

        if (borrowRequest.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu mượn không ở trạng thái chưa duyệt");
        }

        borrowRequest.setRequestStatus(RequestStatus.APPROVED);
        borrowRequestRepository.save(borrowRequest);

        // Có thể thêm thông báo "Đơn hàng đang được chuẩn bị/giao"
    }

    // Hoàn thành giao sách -> Chuyển sang Đang mượn
    @Override
    public BorrowTransaction completeBorrowDelivery(Long requestId, Long librarianId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Thủ thư không tồn tại"));

        if (borrowRequest.getRequestStatus() != RequestStatus.APPROVED) {
            throw new RuntimeException("Yêu cầu mượn không ở trạng thái đang giao");
        }

        // Tạo giao dịch mượn
        BorrowTransaction transaction = new BorrowTransaction();
        transaction.setUser(borrowRequest.getUser());
        transaction.setLibrarian(librarian);
        transaction.setBorrowDate(LocalDateTime.now());

        transaction.setDueDate(LocalDateTime.now().plusDays(DEFAULT_BORROW_DAYS));
        transaction.setStatus(TransactionStatus.BORROWED);
        transaction = borrowTransactionRepository.save(transaction);

        // Thêm sách vào giao dịch
        List<BorrowRequestItem> requestItems = borrowRequestItemRepository.findByBorrowRequest(borrowRequest);
        for (BorrowRequestItem requestItem : requestItems) {
            for (int i = 0; i < requestItem.getQuantity(); i++) {
                BorrowItem borrowItem = new BorrowItem();
                borrowItem.setTransaction(transaction);
                
                List<BookCopy> availableCopies = bookCopyRepository.findByBookIdAndStatus(requestItem.getBook().getId(), BookCopyStatus.AVAILABLE);
                BookCopy assignedCopy;
                if (availableCopies.isEmpty()) {
                    // Tự động tạo bản sao nếu dữ liệu bị thiếu (để fix lỗi inconsistency)
                    assignedCopy = new BookCopy();
                    assignedCopy.setBook(requestItem.getBook());
                    assignedCopy.setBarcode(requestItem.getBook().getIsbn() + "-" + System.currentTimeMillis() + "-" + i);
                    assignedCopy.setStatus(BookCopyStatus.BORROWED);
                    assignedCopy.setCreatedDate(LocalDateTime.now());
                } else {
                    assignedCopy = availableCopies.get(0);
                    assignedCopy.setStatus(BookCopyStatus.BORROWED);
                }
                bookCopyRepository.save(assignedCopy);

                borrowItem.setBookCopy(assignedCopy);
                borrowItemRepository.save(borrowItem);
            }
        }

        // Cập nhật trạng thái yêu cầu sang COMPLETED (Hoàn tất giao)
        borrowRequest.setRequestStatus(RequestStatus.COMPLETED);
        borrowRequestRepository.save(borrowRequest);

        // Thông báo cho user
        notificationService.notifyBorrowApproved(transaction);

        return transaction;
    }

    @Override
    public void createDirectBorrow(Long librarianId, Long userId, List<Long> bookIds, List<Integer> quantities) {
        User librarian = userRepository.findById(librarianId).orElseThrow(() -> new RuntimeException("Thủ thư không tồn tại"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Độc giả không tồn tại"));

        if (bookIds == null || bookIds.isEmpty() || quantities == null || quantities.size() != bookIds.size()) {
            throw new RuntimeException("Danh sách sách không hợp lệ");
        }

        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalBorrowFee = BigDecimal.ZERO;
        int totalBooks = 0;

        for (int i = 0; i < bookIds.size(); i++) {
            Long bookId = bookIds.get(i);
            int qty = quantities.get(i);
            if (qty <= 0) continue;

            Book book = bookRepository.findByIdWithLock(bookId).orElseThrow(() -> new RuntimeException("Truyện không tồn tại"));
            
            if (book.getQuantity() < qty) {
                throw new RuntimeException("Sách '" + book.getTitle() + "' không đủ số lượng (còn " + book.getQuantity() + ")");
            }
            
            BigDecimal deposit = book.getDepositPrice() != null ? book.getDepositPrice() : BigDecimal.ZERO;
            totalDeposit = totalDeposit.add(deposit.multiply(BigDecimal.valueOf(qty)));
            totalBorrowFee = totalBorrowFee.add(deposit.multiply(BigDecimal.valueOf(0.1)).multiply(BigDecimal.valueOf(qty)));
            totalBooks += qty;
        }

        if (totalBooks == 0) {
            throw new RuntimeException("Chưa chọn sách nào để mượn");
        }

        BigDecimal totalAmount = totalDeposit.add(totalBorrowFee);
        // Loại bỏ kiểm tra số dư ví và trừ ví người dùng cho mượn trực tiếp tại quầy

        // 1. Tạo BorrowRequest
        BorrowRequest request = new BorrowRequest();
        request.setUser(user);
        request.setRequestDate(LocalDateTime.now());
        request.setRequestStatus(RequestStatus.COMPLETED);
        request.setDeliveryMethod(DeliveryMethod.PICKUP);
        request.setShippingAddress(null);
        request.setNote("Đơn mượn trực tiếp tại quầy");
        request.setTotalDeposit(totalDeposit);
        request.setShippingFee(BigDecimal.ZERO);
        request = borrowRequestRepository.save(request);

        // 2. Tạo BorrowRequestItem và cập nhật tồn kho Book
        for (int i = 0; i < bookIds.size(); i++) {
            Long bookId = bookIds.get(i);
            int qty = quantities.get(i);
            if (qty <= 0) continue;

            Book book = bookRepository.findById(bookId).get();
            int oldQuantity = book.getQuantity() != null ? book.getQuantity() : 0;
            book.setQuantity(oldQuantity - qty);
            bookRepository.save(book);

            if (oldQuantity >= 20 && book.getQuantity() < 20) {
                notificationService.notifyLowStock(book);
            }

            BorrowRequestItem reqItem = new BorrowRequestItem();
            reqItem.setBorrowRequest(request);
            reqItem.setBook(book);
            reqItem.setQuantity(qty);
            borrowRequestItemRepository.save(reqItem);
        }

        // 3. Tạo BorrowTransaction
        BorrowTransaction transaction = new BorrowTransaction();
        transaction.setUser(user);
        transaction.setLibrarian(librarian);
        transaction.setBorrowDate(LocalDateTime.now());
        transaction.setDueDate(LocalDateTime.now().plusDays(DEFAULT_BORROW_DAYS));
        transaction.setStatus(TransactionStatus.BORROWED);
        transaction = borrowTransactionRepository.save(transaction);

        // 4. Tạo BorrowItem và cập nhật BookCopy
        for (int i = 0; i < bookIds.size(); i++) {
            Long bookId = bookIds.get(i);
            int qty = quantities.get(i);
            if (qty <= 0) continue;

            Book book = bookRepository.findById(bookId).get();

            for (int j = 0; j < qty; j++) {
                BorrowItem borrowItem = new BorrowItem();
                borrowItem.setTransaction(transaction);

                java.util.List<BookCopy> availableCopies = bookCopyRepository.findByBookIdAndStatus(bookId, BookCopyStatus.AVAILABLE);
                BookCopy assignedCopy;
                if (availableCopies.isEmpty()) {
                    assignedCopy = new BookCopy();
                    assignedCopy.setBook(book);
                    assignedCopy.setBarcode(book.getIsbn() + "-" + System.currentTimeMillis() + "-" + j);
                    assignedCopy.setStatus(BookCopyStatus.BORROWED);
                    assignedCopy.setCreatedDate(LocalDateTime.now());
                } else {
                    assignedCopy = availableCopies.get(0);
                    assignedCopy.setStatus(BookCopyStatus.BORROWED);
                }
                bookCopyRepository.save(assignedCopy);

                borrowItem.setBookCopy(assignedCopy);
                borrowItemRepository.save(borrowItem);
            }
        }

        // 5. Gửi thông báo
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String notifContent = String.format("Thủ thư đã tạo đơn mượn trực tiếp (Mã đơn: #%d) với %d cuốn truyện. \n" +
                "- Tổng cọc: %s đ\n" +
                "- Tổng phí thuê: %s đ\n" +
                "- Tổng tiền thanh toán: %s đ\n" +
                "Bạn cần trả truyện trước ngày %s.",
                transaction.getId(), totalBooks, nf.format(totalDeposit), nf.format(totalBorrowFee), nf.format(totalAmount), 
                transaction.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        notificationService.sendNotification(user, "Tạo đơn mượn trực tiếp", notifContent, "/borrow/history");

        // Cộng tiền vào ví ADMIN
        adminWalletService.logTransaction(totalAmount, "BORROW_INCOME", String.format("Nhận thanh toán đơn mượn trực tiếp tại quầy #%d", transaction.getId()), transaction.getId());

        List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
        if (!admins.isEmpty()) {
            User admin = admins.get(0);
            notificationService.sendNotification(admin, "Nhận tiền thanh toán đơn mượn trực tiếp", 
                String.format("Nhận %s đ từ đơn mượn trực tiếp (Mã đơn: #%d) của độc giả %s. Cọc: %s đ, Phí thuê: %s đ. Số dư ví Admin hiện tại: %s đ.", 
                    nf.format(totalAmount), transaction.getId(), user.getFullName() != null ? user.getFullName() : user.getUserName(), 
                    nf.format(totalDeposit), nf.format(totalBorrowFee), nf.format(admin.getBalance())), 
                null);
        }
    }

    // Từ chối yêu cầu mượn sách: cập nhật trạng thái yêu cầu sang REJECTED, đính kèm lý do từ chối, trả lại số lượng sách vào kho và gửi thông báo từ chối cho người dùng
    @Override
    public void rejectBorrowRequest(Long requestId, String reason) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));

        if (borrowRequest.getRequestStatus() != RequestStatus.PENDING && borrowRequest.getRequestStatus() != RequestStatus.APPROVED) {
            throw new RuntimeException("Chỉ có thể từ chối/hủy yêu cầu đang chờ duyệt hoặc đang giao");
        }

        borrowRequest.setRequestStatus(RequestStatus.REJECTED);
        borrowRequest.setRejectionReason(reason);
        borrowRequestRepository.save(borrowRequest);

        // HOÀN LẠI SỐ LƯỢNG KHI TỪ CHỐI
        List<BorrowRequestItem> items = borrowRequestItemRepository.findByBorrowRequest(borrowRequest);
        for (BorrowRequestItem item : items) {
            Book book = item.getBook();
            book.setQuantity(book.getQuantity() + 1);
            bookRepository.save(book);
        }

        // TRỪ TIỀN KHỎI VÍ ADMIN VÌ TỪ CHỐI ĐƠN (thống kê hoàn tiền của thư viện)
        User user = borrowRequest.getUser();
        BigDecimal totalRefund = (borrowRequest.getTotalDeposit() != null ? borrowRequest.getTotalDeposit() : BigDecimal.ZERO)
                             .add(borrowRequest.getShippingFee() != null ? borrowRequest.getShippingFee() : BigDecimal.ZERO);
        
        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            // TRỪ TIỀN KHỎI VÍ ADMIN VÌ TỪ CHỐI ĐƠN
            adminWalletService.logTransaction(totalRefund.negate(), "REFUND_EXPENSE", String.format("Hoàn tiền đơn mượn bị từ chối #%d", borrowRequest.getId()), borrowRequest.getId());

            List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
            if (!admins.isEmpty()) {
                User admin = admins.get(0);
                NumberFormat nfAdmin = NumberFormat.getInstance(new Locale("vi", "VN"));
                notificationService.sendNotification(admin, "Trừ tiền từ chối đơn mượn", 
                    String.format("Trừ %s đ do đơn mượn (Mã đơn: #%d) của độc giả %s bị từ chối. Số dư hiện tại: %s đ.", 
                        nfAdmin.format(totalRefund), borrowRequest.getId(), user.getFullName() != null ? user.getFullName() : user.getUserName(), 
                        nfAdmin.format(admin.getBalance())), 
                    null);
            }
        }

        // Thông báo
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String notifContent = String.format("Đơn mượn truyện (Mã đơn: #%d) của bạn đã bị từ chối.\n" +
                "- Lý do: %s\n" +
                "- Số tiền %s đ (Cọc + Ship) đã được làm thủ tục hoàn trả lại vào tài khoản ngân hàng của bạn.",
                borrowRequest.getId(),
                (reason != null && !reason.trim().isEmpty() ? reason : "Không có lý do"),
                nf.format(totalRefund));
        
        notificationService.sendNotification(user, "Đơn mượn bị từ chối", notifContent, "/borrow/history");
    }

    // Lấy toàn bộ lịch sử giao dịch mượn truyện của một người dùng và trả về dưới dạng danh sách DTO
    @Override
    public Page<BorrowHistoryDTO> getUserBorrowHistory(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Page<BorrowTransaction> transactions = borrowTransactionRepository.findByUser(user, pageable);
        return transactions.map(this::mapToBorrowHistoryDTO);
    }

    @Override
    public Page<CombinedHistoryDTO> getCombinedBorrowHistory(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        
        Page<BorrowRequest> requests = borrowRequestRepository.findByUser(user, pageable);
        return requests.map(req -> {
            List<BorrowRequestItem> items = borrowRequestItemRepository.findByBorrowRequest(req);
            String booksSummary = items.stream()
                .map(item -> item.getBook().getTitle())
                .limit(3)
                .collect(Collectors.joining(", "));
            if (items.size() > 3) booksSummary += "...";

            String reqDate = req.getRequestDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String status = req.getRequestStatus().toString();
            String returnDateStr = "Chưa có";
            Long transactionId = null;

            if (req.getRequestStatus() == RequestStatus.COMPLETED) {
                List<BorrowTransaction> txs = borrowTransactionRepository.findByUser(user, Pageable.unpaged()).getContent();
                for (BorrowTransaction tx : txs) {
                    if (tx.getBorrowDate().isAfter(req.getRequestDate().minusMinutes(5)) && tx.getBorrowDate().isBefore(req.getRequestDate().plusDays(60))) {
                        boolean hasMatchingBook = false;
                        if (!items.isEmpty()) {
                            Long firstReqBookId = items.get(0).getBook().getId();
                            for (BorrowItem txItem : tx.getItems()) {
                                if (txItem.getBookCopy().getBook().getId().equals(firstReqBookId)) {
                                    hasMatchingBook = true;
                                    break;
                                }
                            }
                        } else {
                            hasMatchingBook = true;
                        }

                        if (hasMatchingBook) {
                            transactionId = tx.getId();
                            if (tx.getReturnDate() != null) {
                                returnDateStr = tx.getReturnDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            } else {
                                returnDateStr = tx.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            }
                            status = tx.getStatus().toString();

                            // Kiểm tra nếu có ReturnRequest đang active
                            List<ReturnRequest> returns = returnRequestRepository.findByBorrowTransactionOrderByRequestDateDesc(tx);
                            if (!returns.isEmpty()) {
                                ReturnRequest latestReturn = returns.get(0);
                                if (latestReturn.getRequestStatus() == RequestStatus.PENDING || 
                                    latestReturn.getRequestStatus() == RequestStatus.APPROVED || 
                                    latestReturn.getRequestStatus() == RequestStatus.RETURNING) {
                                    status = "RETURN_" + latestReturn.getRequestStatus().name();
                                }
                            }
                            // Fallback nếu transaction đang là PENDING (do code cũ)
                            if (status.equals("PENDING")) {
                                status = "BORROWED";
                            }
                            break;
                        }
                    }
                }
            }

            // Dịch trạng thái sang tiếng Việt
            switch (status) {
                case "PENDING": status = "Chờ xử lý"; break;
                case "APPROVED": status = "Đang gửi hàng"; break;
                case "COMPLETED": status = "Hoàn thành"; break;
                case "BORROWED": status = "Đang mượn"; break;
                case "RETURNED": status = "Đã trả"; break;
                case "OVERDUE": status = "Quá hạn"; break;
                case "REJECTED": status = "Từ chối"; break;
                case "CANCELLED": status = "Đã hủy"; break;
                case "RETURN_PENDING": status = "Chờ lấy truyện trả"; break;
                case "RETURN_APPROVED": status = "Shipper đang đi lấy"; break;
                case "RETURN_RETURNING": status = "Đang mang về thư viện"; break;
            }

            return new CombinedHistoryDTO(
                req.getId(),
                booksSummary,
                reqDate,
                returnDateStr,
                status,
                req.getTotalDeposit(),
                req.getShippingFee(),
                (req.getTotalDeposit() != null ? req.getTotalDeposit() : BigDecimal.ZERO).add(req.getShippingFee() != null ? req.getShippingFee() : BigDecimal.ZERO),
                transactionId
            );
        });
    }

    // Chuyển đổi dữ liệu từ thực thể BorrowTransaction sang đối tượng trung chuyển BorrowHistoryDTO
    private BorrowHistoryDTO mapToBorrowHistoryDTO(BorrowTransaction transaction) {
        List<BorrowItem> items = borrowItemRepository.findByTransaction(transaction);
        Long bookId = items.isEmpty() ? null : items.get(0).getBookCopy().getBook().getId();
        String bookName = items.isEmpty() ? "Không xác định" : items.get(0).getBookCopy().getBook().getTitle();
        String bookImage = items.isEmpty() ? null : items.get(0).getBookCopy().getBook().getImage();
        
        String returnRequestStatus = null;
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            List<ReturnRequest> requests = returnRequestRepository.findByBorrowTransactionOrderByRequestDateDesc(transaction);
            if (!requests.isEmpty()) {
                returnRequestStatus = requests.get(0).getRequestStatus().toString();
            }
        }

        return new BorrowHistoryDTO(
                transaction.getId(),
                bookId,
                bookName,
                bookImage,
                transaction.getBorrowDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                transaction.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                transaction.getReturnDate() != null ? transaction.getReturnDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Chưa trả",
                transaction.getStatus().toString(),
                returnRequestStatus
        );
    }

    // Lấy thông tin chi tiết của một giao dịch mượn truyện theo ID giao dịch
    @Override
    public Optional<BorrowTransaction> getBorrowTransactionDetail(Long transactionId) {
        return borrowTransactionRepository.findById(transactionId);
    }

    // Thủ thư xác nhận người dùng trả sách trực tiếp: Cập nhật ngày trả, đổi trạng thái sang RETURNED, hoàn trả lại số lượng sách vào kho lưu trữ và gửi thông báo trả thành công
    @Override
    public void returnBorrowItems(Long transactionId, Long librarianId, List<Long> itemIds, BigDecimal returnShippingFee) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));
        User librarian = null;
        if (librarianId != null) {
            librarian = userRepository.findById(librarianId).orElseThrow(() -> new RuntimeException("Thủ thư không tồn tại"));
        }

        if (transaction.getStatus() == TransactionStatus.RETURNED) {
            throw new RuntimeException("Giao dịch này đã được hoàn thành trả truyện trước đó");
        }

        List<BorrowItem> allItems = borrowItemRepository.findByTransaction(transaction);
        List<BorrowItem> itemsToReturn;
        
        if (itemIds == null || itemIds.isEmpty()) {
            itemsToReturn = allItems.stream().filter(i -> i.getReturnDate() == null).collect(java.util.stream.Collectors.toList());
        } else {
            itemsToReturn = allItems.stream().filter(i -> itemIds.contains(i.getId()) && i.getReturnDate() == null).collect(java.util.stream.Collectors.toList());
        }

        if (itemsToReturn.isEmpty()) {
            throw new RuntimeException("Không có sách nào cần trả hoặc sách đã được trả");
        }

        // Cập nhật thông tin trả và TĂNG số lượng trong kho
        for (BorrowItem item : itemsToReturn) {
            item.setReturnDate(LocalDateTime.now());
            borrowItemRepository.save(item);

            BookCopy copy = item.getBookCopy();
            copy.setStatus(BookCopyStatus.AVAILABLE);
            bookCopyRepository.save(copy);
            
            Book book = copy.getBook();
            book.setQuantity(book.getQuantity() + 1);
            bookRepository.save(book);
        }

        boolean allReturned = true;
        for (BorrowItem item : allItems) {
            if (item.getReturnDate() == null) {
                allReturned = false;
                break;
            }
        }

        long lateFine = 0;
        if (allReturned) {
            transaction.setReturnDate(LocalDateTime.now());
            if (librarian != null) transaction.setLibrarian(librarian);
            transaction.setStatus(TransactionStatus.RETURNED);
            borrowTransactionRepository.save(transaction);
            
            lateFine = calculateLateFine(transaction.getId());
        } else {
            if (LocalDateTime.now().isAfter(transaction.getDueDate())) {
                transaction.setStatus(TransactionStatus.OVERDUE);
            } else {
                transaction.setStatus(TransactionStatus.BORROWED);
            }
            borrowTransactionRepository.save(transaction);
        }

        // Hoàn tiền cọc sau khi trừ phí mượn và phí trễ hạn (nếu trả xong)
        User user = transaction.getUser();
        BigDecimal originalDeposit = itemsToReturn.stream()
                .map(item -> item.getBookCopy().getBook().getDepositPrice() != null ? item.getBookCopy().getBook().getDepositPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalBorrowFee = itemsToReturn.stream()
                .map(item -> {
                    BigDecimal depositPrice = item.getBookCopy().getBook().getDepositPrice() != null ? item.getBookCopy().getBook().getDepositPrice() : BigDecimal.ZERO;
                    return depositPrice.multiply(BigDecimal.valueOf(0.1));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal shippingDeduction = returnShippingFee != null ? returnShippingFee : BigDecimal.ZERO;
        BigDecimal refundAmount = originalDeposit.subtract(totalBorrowFee).subtract(BigDecimal.valueOf(lateFine)).subtract(shippingDeduction);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) refundAmount = BigDecimal.ZERO; // Không hoàn âm

        // Trừ tiền hoàn trả từ ví Admin (chi hoàn tiền ngân hàng cho user)
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            adminWalletService.logTransaction(refundAmount.negate(), "REFUND_EXPENSE", String.format("Hoàn trả tiền cọc trả truyện #%d", transaction.getId()), transaction.getId());

            List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
            if (!admins.isEmpty()) {
                User admin = admins.get(0);
                
                NumberFormat nfAdmin = NumberFormat.getInstance(new Locale("vi", "VN"));
                String adminNotifContent = String.format("Hoàn trả %s đ tiền cọc cho độc giả %s (Giao dịch: #%d). " +
                    "Cọc trả: %s đ, Trừ phí thuê: %s đ, Phạt: %s đ, Phí ship: %s đ. Số dư mới: %s đ.",
                    nfAdmin.format(refundAmount),
                    user.getFullName() != null ? user.getFullName() : user.getUserName(),
                    transaction.getId(),
                    nfAdmin.format(originalDeposit), nfAdmin.format(totalBorrowFee), nfAdmin.format(lateFine), nfAdmin.format(shippingDeduction), nfAdmin.format(admin.getBalance()));
                notificationService.sendNotification(admin, "Trừ ví hoàn tiền trả truyện", adminNotifContent, null);
            }
        } else if (refundAmount.compareTo(BigDecimal.ZERO) == 0 && (totalBorrowFee.compareTo(BigDecimal.ZERO) > 0 || lateFine > 0)) {
            // Không hoàn tiền, nhưng gửi thông báo là thu trọn cọc
            List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
            if (!admins.isEmpty()) {
                User admin = admins.get(0);
                NumberFormat nfAdmin = NumberFormat.getInstance(new Locale("vi", "VN"));
                String adminNotifContent = String.format("Đơn trả truyện (Giao dịch: #%d) của %s không phát sinh hoàn tiền (Giữ trọn cọc %s đ do Phí thuê: %s đ, Phạt: %s đ). Số dư hiện tại: %s đ.",
                    transaction.getId(),
                    user.getFullName() != null ? user.getFullName() : user.getUserName(),
                    nfAdmin.format(originalDeposit), nfAdmin.format(totalBorrowFee), nfAdmin.format(lateFine), nfAdmin.format(admin.getBalance()));
                notificationService.sendNotification(admin, "Hoàn tất đơn trả truyện", adminNotifContent, null);
            }
        }

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String notifContent = String.format("Đã xử lý trả %d cuốn. Cọc: %s đ, Phí thuê: %s đ, Phạt: %s đ, Ship trả: %s đ. Số tiền thực nhận %s đ đã được chuyển hoàn trả lại tài khoản ngân hàng của bạn.",
            itemsToReturn.size(), nf.format(originalDeposit), nf.format(totalBorrowFee), nf.format(lateFine), nf.format(shippingDeduction), nf.format(refundAmount));
            
        notificationService.sendNotification(user, "Xử lý trả truyện thành công", notifContent, "/borrow/history");

        if (allReturned) {
            // Thông báo
            notificationService.notifyBorrowReturned(transaction);
        }
    }

    // Tính tiền phạt
    public long calculateLateFine(Long transactionId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        if (transaction.getReturnDate() == null ||
                (transaction.getStatus() != TransactionStatus.OVERDUE && transaction.getStatus() != TransactionStatus.RETURNED)) {
            return 0;
        }

        LocalDateTime returnDate = transaction.getReturnDate();
        long daysOverdue = ChronoUnit.DAYS.between(transaction.getDueDate(), returnDate);

        if (daysOverdue > 0) {
            return daysOverdue * DAILY_FINE;
        }

        return 0;
    }

    // Lấy danh sách các giao dịch mượn sách chưa hoàn tất trả
    @Override
    public Page<BorrowHistoryDTO> getActiveBorrows(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        List<TransactionStatus> activeStatuses = List.of(TransactionStatus.BORROWED, TransactionStatus.OVERDUE, TransactionStatus.PENDING);
        Page<BorrowTransaction> transactions = borrowTransactionRepository.findByUserAndStatusIn(user, activeStatuses, pageable);
        return transactions.map(this::mapToBorrowHistoryDTO);
    }

    @Override
    public Page<BorrowTransaction> getActiveTransactionsPaged(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        List<TransactionStatus> activeStatuses = List.of(TransactionStatus.BORROWED, TransactionStatus.OVERDUE, TransactionStatus.PENDING);
        return borrowTransactionRepository.findByUserAndStatusIn(user, activeStatuses, pageable);
    }

    // Kiểm tra xem một giao dịch mượn truyện cụ thể có bị quá hạn trả sách hay không
    @Override
    public boolean isOverdue(Long transactionId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        if (transaction.getStatus() != TransactionStatus.BORROWED) {
            return false;
        }

        return LocalDateTime.now().isAfter(transaction.getDueDate());
    }

    // Tự động cảnh báo sách sắp hết hạn 1 ngày
    @Scheduled(cron = "0 0 8 * * ?") // Chạy vào 8h sáng mỗi ngày
    public void warnAlmostOverdueTransactions() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.toLocalDate().atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.toLocalDate().atTime(23, 59, 59);

        // Tìm các giao dịch có ngày trả vào ngày mai
        List<BorrowTransaction> transactions = borrowTransactionRepository.findByStatusAndDueDateBetween(
                TransactionStatus.BORROWED, startOfTomorrow, endOfTomorrow);
        
        for (BorrowTransaction tx : transactions) {
            notificationService.sendNotification(
                    tx.getUser(),
                    "Truyện mượn sắp hết hạn!",
                    "Giao dịch mượn mã #" + tx.getId() + " của bạn sẽ hết hạn vào ngày mai. Vui lòng trả truyện hoặc gia hạn để tránh phí phạt 7,000đ/ngày.",
                    "/borrow/active"
            );
        }
    }

    // Gia hạn sách
    @Override
    public void extendBorrowTransaction(Long transactionId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        if (transaction.getStatus() != TransactionStatus.BORROWED) {
            throw new RuntimeException("Chỉ có thể gia hạn truyện đang mượn");
        }
        
        User user = transaction.getUser();
        List<BorrowItem> items = borrowItemRepository.findByTransaction(transaction);
        BigDecimal extensionFee = items.stream()
                .map(item -> {
                    BigDecimal depositPrice = item.getBookCopy().getBook().getDepositPrice() != null ? item.getBookCopy().getBook().getDepositPrice() : BigDecimal.ZERO;
                    return depositPrice.multiply(BigDecimal.valueOf(0.1));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Ghi nhận doanh thu gia hạn vào ví Admin
        adminWalletService.logTransaction(extensionFee, "BORROW_INCOME", String.format("Nhận thanh toán gia hạn mượn sách #%d", transaction.getId()), transaction.getId());
        
        transaction.setDueDate(transaction.getDueDate().plusDays(DEFAULT_BORROW_DAYS));
        borrowTransactionRepository.save(transaction);
        
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        notificationService.sendNotification(
                user,
                "Gia hạn mượn sách",
                "Bạn đã gia hạn mượn sách thành công. Thời gian mượn được cộng thêm 7 ngày.\n" +
                "- Hạn trả mới: " + transaction.getDueDate().toLocalDate() + "\n" +
                "- Phí gia hạn: " + nf.format(extensionFee) + " đ đã được thanh toán trực tiếp.",
                "/borrow/history"
        );
    }

    //các giao dịch mượn truyện bị quá hạn của user
    @Override
    public List<BorrowTransaction> getOverdueBooks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return borrowTransactionRepository.findUserOverdueTransactions(userId);
    }

    // các yêu cầu mượn truyện của user đang chờ xét
    @Override
    public List<BorrowRequest> getAllPendingRequests() {
        return borrowRequestRepository.findPendingRequests();
    }

    @Override
    public Page<BorrowRequest> getPendingRequests(String query, Pageable pageable) {
        return borrowRequestRepository.findPendingRequestsWithSearch(query, pageable);
    }

    @Override
    public Page<BorrowRequest> getApprovedRequests(String query, Pageable pageable) {
        return borrowRequestRepository.findApprovedRequestsWithSearch(query, pageable);
    }

    //các giao dịch mượn truyện đang hoạt động
    @Override
    public List<BorrowTransaction> getAllActiveBorrows() {
        return borrowTransactionRepository.findByStatusIn(List.of(TransactionStatus.BORROWED, TransactionStatus.OVERDUE, TransactionStatus.PENDING));
    }

    @Override
    public Page<Object[]> getActiveBorrowers(String query, Pageable pageable) {
        return borrowTransactionRepository.findActiveBorrowers(query, pageable);
    }

    //tạo yêu cầu trả truyện
    @Override
    public void createReturnRequest(Long userId, Long transactionId, LocalDateTime returnDateTime, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));

        if (transaction.getStatus() != TransactionStatus.BORROWED && transaction.getStatus() != TransactionStatus.OVERDUE) {
            throw new RuntimeException("Giao dịch không ở trạng thái có thể trả");
        }

        ReturnRequest request = new ReturnRequest();
        request.setUser(user);
        request.setBorrowTransaction(transaction);
        request.setRequestDate(LocalDateTime.now());
        request.setReturnDateTime(returnDateTime);
        request.setNote(note);
        request.setRequestStatus(RequestStatus.PENDING);

        returnRequestRepository.save(request);

        // Không đổi trạng thái giao dịch
        // transaction.setStatus(TransactionStatus.PENDING);
        // borrowTransactionRepository.save(transaction);

        // Thông báo cho thủ thư
        notificationService.notifyReviewReturnRequest(request);
    }

    private BorrowTransaction findTransactionForRequest(BorrowRequest req) {
        if (req.getRequestStatus() == RequestStatus.COMPLETED) {
            List<BorrowRequestItem> items = borrowRequestItemRepository.findByBorrowRequest(req);
            List<BorrowTransaction> txs = borrowTransactionRepository.findByUser(req.getUser(), Pageable.unpaged()).getContent();
            for (BorrowTransaction tx : txs) {
                if (tx.getBorrowDate().isAfter(req.getRequestDate().minusMinutes(5)) && tx.getBorrowDate().isBefore(req.getRequestDate().plusDays(60))) {
                    boolean hasMatchingBook = false;
                    if (!items.isEmpty()) {
                        Long firstReqBookId = items.get(0).getBook().getId();
                        for (BorrowItem txItem : tx.getItems()) {
                            if (txItem.getBookCopy().getBook().getId().equals(firstReqBookId)) {
                                hasMatchingBook = true;
                                break;
                            }
                        }
                    } else {
                        hasMatchingBook = true;
                    }
                    if (hasMatchingBook) return tx;
                }
            }
        }
        return null;
    }

    @Override
    public void createPartialReturnRequest(Long userId, Long requestId, java.util.Map<Long, Integer> returnItems, String returnMethod, String returnAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));
        
        BorrowTransaction transaction = findTransactionForRequest(borrowRequest);
        if (transaction == null || (transaction.getStatus() != TransactionStatus.BORROWED && transaction.getStatus() != TransactionStatus.OVERDUE)) {
            throw new RuntimeException("Đơn mượn không thể trả lúc này.");
        }

        ReturnRequest request = new ReturnRequest();
        request.setUser(user);
        request.setBorrowTransaction(transaction);
        request.setRequestDate(LocalDateTime.now());
        request.setReturnDateTime(LocalDateTime.now().plusDays(1)); // Mặc định trả trong vòng 1 ngày tới
        request.setRequestStatus(RequestStatus.PENDING);
        request.setReturnMethod(com.librarymanagementsystem.model.borrow.status.DeliveryMethod.valueOf(returnMethod));
        
        if ("SHIPPING".equals(returnMethod)) {
            request.setShippingAddress(returnAddress);
            request.setShippingFee(BigDecimal.valueOf(25000)); // Giả lập phí ship 25k
        } else {
            request.setShippingFee(BigDecimal.ZERO);
        }

        List<ReturnRequestItem> returnRequestItems = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, Integer> entry : returnItems.entrySet()) {
            ReturnRequestItem item = new ReturnRequestItem();
            item.setReturnRequest(request);
            item.setBook(bookRepository.findById(entry.getKey()).orElse(null));
            item.setQuantity(entry.getValue());
            returnRequestItems.add(item);
        }
        request.setReturnItems(returnRequestItems);

        returnRequestRepository.save(request);

        // Không đổi trạng thái transaction thành PENDING để vẫn giữ sách trong danh sách đang mượn
        // transaction.setStatus(TransactionStatus.PENDING);
        // borrowTransactionRepository.save(transaction);
        
        // Thông báo
        notificationService.notifyReviewReturnRequest(request);
    }

    @Override
    public void extendBorrowRequest(Long userId, Long requestId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu mượn không tồn tại"));
        
        BorrowTransaction transaction = findTransactionForRequest(borrowRequest);
        if (transaction == null || transaction.getStatus() != TransactionStatus.BORROWED) {
            throw new RuntimeException("Không thể gia hạn đơn này.");
        }
        
        extendBorrowTransaction(transaction.getId());
    }

    @Override
    public ReturnRequest getReturnRequestForBorrowRequest(Long requestId) {
        List<ReturnRequest> returns = getAllReturnRequestsForBorrowRequest(requestId);
        if (returns != null && !returns.isEmpty()) {
            return returns.get(0);
        }
        return null;
    }

    @Override
    public List<ReturnRequest> getAllReturnRequestsForBorrowRequest(Long requestId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId).orElse(null);
        if (borrowRequest == null) return java.util.Collections.emptyList();
        BorrowTransaction transaction = findTransactionForRequest(borrowRequest);
        if (transaction == null) return java.util.Collections.emptyList();
        
        return returnRequestRepository.findByBorrowTransactionOrderByRequestDateDesc(transaction);
    }

    @Override
    public BorrowTransaction getTransactionForBorrowRequest(Long requestId) {
        BorrowRequest borrowRequest = borrowRequestRepository.findById(requestId).orElse(null);
        if (borrowRequest == null) return null;
        return findTransactionForRequest(borrowRequest);
    }

    // toàn bộ yêu cầu trả sách đang chờ phê duyệt or đã phê duyệt
    @Override
    public List<ReturnRequest> getAllPendingReturnRequests() {
        return returnRequestRepository.findByRequestStatusInOrderByRequestDateDesc(List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.RETURNING));
    }

    @Override
    public Page<ReturnRequest> getPendingReturnRequests(String query, Pageable pageable) {
        return returnRequestRepository.findPendingReturnRequestsWithSearch(query, pageable);
    }

    // Thủ thư phê duyệt yêu cầu trả truyện
    public void approveReturnRequest(Long requestId, Long librarianId) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        request.setRequestStatus(RequestStatus.APPROVED);
        returnRequestRepository.save(request);

        // Thông báo cho user
        notificationService.notifyReturnApproved(request);
    }

    @Override
    public void receiveReturnRequest(Long requestId, Long librarianId) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        if (request.getRequestStatus() != RequestStatus.APPROVED) {
            throw new RuntimeException("Yêu cầu phải được duyệt trước khi nhận hàng");
        }

        request.setRequestStatus(RequestStatus.RETURNING);
        returnRequestRepository.save(request);
        // Có thể thêm thông báo "Shipper đã nhận truyện và đang mang về" nếu cần
    }

    // Thủ thư hoàn tất yêu cầu trả truyện
    @Override
    @Transactional
    public void completeReturnRequest(Long requestId, Long librarianId) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        if (request.getRequestStatus() != RequestStatus.RETURNING && request.getRequestStatus() != RequestStatus.APPROVED) {
            throw new RuntimeException("Yêu cầu phải được duyệt và nhận hàng trước khi hoàn tất");
        }

        request.setRequestStatus(RequestStatus.COMPLETED);
        returnRequestRepository.save(request);

        // Lấy danh sách ID của các BorrowItem cần trả dựa trên số lượng yêu cầu (ReturnRequestItem)
        List<Long> itemIds = new java.util.ArrayList<>();
        if (request.getReturnItems() != null && !request.getReturnItems().isEmpty()) {
            BorrowTransaction tx = request.getBorrowTransaction();
            for (com.librarymanagementsystem.model.borrow.ReturnRequestItem reqItem : request.getReturnItems()) {
                int qtyNeeded = reqItem.getQuantity();
                if (qtyNeeded <= 0) continue;
                Long bookId = reqItem.getBook().getId();
                
                int found = 0;
                for (com.librarymanagementsystem.model.borrow.BorrowItem bi : tx.getItems()) {
                    if (bi.getReturnDate() == null && bi.getBookCopy().getBook().getId().equals(bookId)) {
                        itemIds.add(bi.getId());
                        found++;
                        if (found >= qtyNeeded) break;
                    }
                }
            }
        } else {
            // Dự phòng cho các đơn trả cũ dùng format Note
            if (request.getNote() != null && request.getNote().startsWith("Trả ID truyện: [")) {
                try {
                    int start = request.getNote().indexOf("[") + 1;
                    int end = request.getNote().indexOf("]");
                    String idsStr = request.getNote().substring(start, end);
                    if (!idsStr.trim().isEmpty()) {
                        itemIds = java.util.Arrays.stream(idsStr.split(","))
                                .map(String::trim)
                                .map(Long::parseLong)
                                .collect(java.util.stream.Collectors.toList());
                    }
                } catch (Exception e) {
                    // Ignore parse errors, treat as return all
                }
            }
        }


        // Hoàn tất việc trả truyện
        returnBorrowItems(request.getBorrowTransaction().getId(), librarianId, itemIds, request.getShippingFee());

    }

    // Thủ thư từ chối yêu cầu trả truyện
    public void rejectReturnRequest(Long requestId, String reason) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        request.setRequestStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        returnRequestRepository.save(request);

        // Khôi phục trạng thái giao dịch
        BorrowTransaction transaction = request.getBorrowTransaction();
        if (LocalDateTime.now().isAfter(transaction.getDueDate())) {
            transaction.setStatus(TransactionStatus.OVERDUE);
        } else {
            transaction.setStatus(TransactionStatus.BORROWED);
        }
        borrowTransactionRepository.save(transaction);
        
        // Thông báo
        notificationService.notifyReturnRejected(request);
    }

    @Override
    public void cancelReturnRequest(Long requestId, Long userId) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu trả không tồn tại"));
        
        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy yêu cầu này");
        }
        
        if (request.getRequestStatus() != RequestStatus.PENDING && request.getRequestStatus() != RequestStatus.APPROVED) {
            throw new RuntimeException("Chỉ có thể hủy yêu cầu trả truyện khi chưa giao cho shipper");
        }

        request.setRequestStatus(RequestStatus.CANCELLED);
        returnRequestRepository.save(request);
    }

    @Override
    public org.springframework.data.domain.Page<CombinedHistoryDTO> getCombinedBorrowHistory(Long userId, String keyword, Long bookId, String statusFilter, String sortOption, Pageable pageable) {
        // Fetch all history first (since it is a combined list of request + transaction mapped in memory)
        List<CombinedHistoryDTO> allHistory = new ArrayList<>(getCombinedBorrowHistory(userId, Pageable.unpaged()).getContent());
        
        // Filtering
        if (bookId != null || (keyword != null && !keyword.trim().isEmpty())) {
            String lowerKw = keyword != null ? keyword.toLowerCase() : "";
            allHistory = allHistory.stream().filter(dto -> {
                boolean matchBookId = false;
                if (bookId != null && dto.getBooksSummary() != null) {
                    try {
                        String bookTitle = bookRepository.findById(bookId).get().getTitle();
                        matchBookId = dto.getBooksSummary().contains(bookTitle);
                    } catch (Exception e) {}
                }
                
                boolean matchKeyword = false;
                if (keyword != null && !keyword.trim().isEmpty() && dto.getBooksSummary() != null) {
                    matchKeyword = dto.getBooksSummary().toLowerCase().contains(lowerKw);
                }
                
                if (bookId != null && (keyword == null || keyword.trim().isEmpty())) {
                    return matchBookId;
                } else if (bookId == null && keyword != null && !keyword.trim().isEmpty()) {
                    return matchKeyword;
                } else {
                    return matchBookId || matchKeyword;
                }
            }).collect(Collectors.toList());
        }
        
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            allHistory = allHistory.stream().filter(dto -> dto.getStatus().equals(statusFilter)).collect(Collectors.toList());
        }
        
        // Sorting
        if (sortOption != null) {
            switch(sortOption) {
                case "newest":
                    allHistory.sort((a,b) -> b.getRequestId().compareTo(a.getRequestId()));
                    break;
                case "return_date":
                    allHistory.sort((a,b) -> {
                        if(a.getReturnDate().equals("Chưa có")) return 1;
                        if(b.getReturnDate().equals("Chưa có")) return -1;
                        try {
                            LocalDate d1 = LocalDate.parse(a.getReturnDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            LocalDate d2 = LocalDate.parse(b.getReturnDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            return d2.compareTo(d1); // Nearest return date
                        } catch(Exception e) { return 0; }
                    });
                    break;
            }
        } else {
            // Default sort by request date
            allHistory.sort((a,b) -> b.getRequestId().compareTo(a.getRequestId()));
        }
        
        int pageSize = pageable.getPageSize();
        int totalItems = allHistory.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageSize, totalItems);
        
        List<CombinedHistoryDTO> paginated = new ArrayList<>();
        if (start < totalItems) {
            paginated = allHistory.subList(start, end);
        }
        
        return new PageImpl<>(paginated, pageable, totalItems);
    }

    @Override
    public Map<String, Object> getBorrowRequestDetailForUser(Long requestId, Long userId) {
        BorrowRequest request = getBorrowRequestDetail(requestId)
                .orElseThrow(() -> new RuntimeException("Đơn mượn không tồn tại"));

        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem thông tin này");
        }

        ReturnRequest returnRequest = getReturnRequestForBorrowRequest(requestId);
        BorrowTransaction transaction = getTransactionForBorrowRequest(requestId);
        
        List<java.util.Map<String, Object>> returnRequestDetails = getReturnRequestDetails(requestId);
        
        Map<Long, Integer> returnedQuantities = new HashMap<>();
        Map<Long, Integer> unreturnedQuantities = new HashMap<>();
        if (transaction != null) {
            for (BorrowItem bi : transaction.getItems()) {
                Long bookId = bi.getBookCopy().getBook().getId();
                if (bi.getReturnDate() != null) {
                    returnedQuantities.put(bookId, returnedQuantities.getOrDefault(bookId, 0) + 1);
                } else {
                    unreturnedQuantities.put(bookId, unreturnedQuantities.getOrDefault(bookId, 0) + 1);
                }
            }
        }
        
        Map<String, Object> modelData = new HashMap<>();
        modelData.put("request", request);
        modelData.put("returnRequest", returnRequest);
        modelData.put("transaction", transaction);
        modelData.put("returnRequestDetails", returnRequestDetails);
        modelData.put("returnedQuantities", returnedQuantities);
        modelData.put("unreturnedQuantities", unreturnedQuantities);
        
        if (returnRequest != null && !returnRequestDetails.isEmpty()) {
            Map<String, Object> latestDetail = returnRequestDetails.get(returnRequestDetails.size() - 1);
            modelData.put("returnOriginalDeposit", latestDetail.get("originalDeposit"));
            modelData.put("returnBorrowFee", latestDetail.get("borrowFee"));
            modelData.put("returnLateFine", latestDetail.get("lateFine"));
            modelData.put("returnRefundAmount", latestDetail.get("refundAmount"));
        } else {
            modelData.put("returnOriginalDeposit", BigDecimal.ZERO);
            modelData.put("returnBorrowFee", BigDecimal.ZERO);
            modelData.put("returnLateFine", BigDecimal.ZERO);
            modelData.put("returnRefundAmount", BigDecimal.ZERO);
        }
        
        return modelData;
    }

    @Override
    public List<Map<String, Object>> getReturnRequestDetails(Long requestId) {
        BorrowRequest request = getBorrowRequestDetail(requestId)
                .orElseThrow(() -> new RuntimeException("Đơn mượn không tồn tại"));
        BorrowTransaction transaction = getTransactionForBorrowRequest(requestId);
        
        List<ReturnRequest> allReturnRequests = getAllReturnRequestsForBorrowRequest(requestId);
        List<java.util.Map<String, Object>> returnRequestDetails = new ArrayList<>();
        
        if (allReturnRequests != null && !allReturnRequests.isEmpty() && transaction != null) {
            BigDecimal currentTotalDeposit = request.getTotalDeposit() != null ? request.getTotalDeposit() : BigDecimal.ZERO;
            for (int i = 0; i < allReturnRequests.size(); i++) {
                ReturnRequest rr = allReturnRequests.get(allReturnRequests.size() - 1 - i);
                BigDecimal originalDeposit = BigDecimal.ZERO;
                int returnQuantity = 0;
                if (rr.getReturnItems() != null) {
                    for (ReturnRequestItem item : rr.getReturnItems()) {
                        if (item.getBook() != null && item.getBook().getDepositPrice() != null) {
                            BigDecimal dp = item.getBook().getDepositPrice();
                            originalDeposit = originalDeposit.add(dp.multiply(BigDecimal.valueOf(item.getQuantity())));
                        }
                        returnQuantity += item.getQuantity();
                    }
                }
                
                BigDecimal totalBorrowFee = BigDecimal.ZERO;
                if (rr.getReturnItems() != null) {
                    for (ReturnRequestItem item : rr.getReturnItems()) {
                        if (item.getBook() != null && item.getBook().getDepositPrice() != null) {
                            BigDecimal dp = item.getBook().getDepositPrice();
                            BigDecimal fee = dp.multiply(BigDecimal.valueOf(0.1)).multiply(BigDecimal.valueOf(item.getQuantity()));
                            totalBorrowFee = totalBorrowFee.add(fee);
                        }
                    }
                }
                BigDecimal lateFine = BigDecimal.ZERO;
                if (rr.getRequestStatus() != RequestStatus.COMPLETED) {
                    lateFine = BigDecimal.valueOf(calculateLateFine(transaction.getId()));
                }
                
                BigDecimal returnShippingFee = rr.getShippingFee() != null ? rr.getShippingFee() : BigDecimal.ZERO;
                BigDecimal refundAmount = originalDeposit.subtract(totalBorrowFee).subtract(lateFine).subtract(returnShippingFee);
                if (refundAmount.compareTo(BigDecimal.ZERO) < 0) refundAmount = BigDecimal.ZERO;
                
                BigDecimal remainingDeposit = currentTotalDeposit.subtract(originalDeposit);
                if (remainingDeposit.compareTo(BigDecimal.ZERO) < 0) remainingDeposit = BigDecimal.ZERO;
                currentTotalDeposit = remainingDeposit;
                
                java.util.Map<String, Object> detail = new java.util.HashMap<>();
                detail.put("returnRequest", rr);
                detail.put("originalDeposit", originalDeposit);
                detail.put("borrowFee", totalBorrowFee);
                detail.put("lateFine", lateFine);
                detail.put("shippingFee", returnShippingFee);
                detail.put("refundAmount", refundAmount);
                detail.put("returnQuantity", returnQuantity);
                detail.put("remainingDeposit", remainingDeposit);
                detail.put("index", i + 1);
                
                returnRequestDetails.add(detail);
            }
        }
        return returnRequestDetails;
    }

    @Override
    public List<java.util.Map<String, Object>> getActiveBooksGrouped(Long userId) {
        List<BorrowTransaction> activeTxs = getActiveTransactionsPaged(userId, Pageable.unpaged()).getContent();
        Map<Long, java.util.Map<String, Object>> groupedBooks = new HashMap<>();
        for (BorrowTransaction tx : activeTxs) {
            for (BorrowItem item : tx.getItems()) {
                if (item.getReturnDate() == null) {
                    Long bookId = item.getBookCopy().getBook().getId();
                    if (!groupedBooks.containsKey(bookId)) {
                       Map<String, Object> group = new HashMap<>();
                        group.put("book", item.getBookCopy().getBook());
                        group.put("quantity", 0);
                        groupedBooks.put(bookId, group);
                    }
                    Map<String, Object> group = groupedBooks.get(bookId);
                    group.put("quantity", (int)group.get("quantity") + 1);
                }
            }
        }
        return new ArrayList<>(groupedBooks.values());
    }

    @Override
    public List<java.util.Map<String, Object>> getGroupedItemsForTransaction(Long transactionId) {
        BorrowTransaction transaction = getBorrowTransactionDetail(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch mượn không tồn tại"));
                
        Map<Long, java.util.Map<String, Object>> grouped = new HashMap<>();
        for (BorrowItem item : transaction.getItems()) {
            Long bookId = item.getBookCopy().getBook().getId();
            if (!grouped.containsKey(bookId)) {
               Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("book", item.getBookCopy().getBook());
                groupInfo.put("totalCount", 0);
                groupInfo.put("returnedCount", 0);
                groupInfo.put("unreturnedIds", new ArrayList<Long>());
                grouped.put(bookId, groupInfo);
            }
            Map<String, Object> groupInfo = grouped.get(bookId);
            groupInfo.put("totalCount", (int) groupInfo.get("totalCount") + 1);
            if (item.getReturnDate() != null) {
                groupInfo.put("returnedCount", (int) groupInfo.get("returnedCount") + 1);
            } else {
                ((List<Long>) groupInfo.get("unreturnedIds")).add(item.getId());
            }
        }
        return new ArrayList<>(grouped.values());
    }

    @Override
    public List<Long> getPendingBookIdsByUser(Long userId) {
        List<Long> pendingBookIds = new ArrayList<>();
        List<BorrowRequest> userRequests = getUserBorrowRequests(userId);
        if (userRequests != null) {
            for (BorrowRequest req : userRequests) {
                if (req.getRequestStatus() == RequestStatus.PENDING) {
                    for (BorrowRequestItem item : req.getBorrowRequestItems()) {
                        pendingBookIds.add(item.getBook().getId());
                    }
                }
            }
        }
        return pendingBookIds;
    }
}
