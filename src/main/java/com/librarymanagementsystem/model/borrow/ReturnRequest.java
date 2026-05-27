package com.librarymanagementsystem.model.borrow;

import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import com.librarymanagementsystem.model.borrow.status.DeliveryMethod;
import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Data
@Table(name = "return_requests")
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private BorrowTransaction borrowTransaction;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "return_date_time")
    private LocalDateTime returnDateTime;  // Ngày giờ user muốn đến trả

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", columnDefinition = "VARCHAR(255)")
    private RequestStatus requestStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;  // Lý do từ chối từ librarian

    @Enumerated(EnumType.STRING)
    @Column(name = "return_method")
    private DeliveryMethod returnMethod;

    @Column(name = "shipping_fee", precision = 15, scale = 2)
    @Max(value = 10000000, message = "Phí ship không được vượt quá 10 triệu VNĐ")
    @Min(value = 0, message = "Phí ship không được nhỏ hơn 0")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnRequestItem> returnItems;
}