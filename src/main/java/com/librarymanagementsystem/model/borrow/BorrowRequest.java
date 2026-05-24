package com.librarymanagementsystem.model.borrow;

import com.librarymanagementsystem.model.borrow.status.DeliveryMethod;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "borrow_requests")
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", columnDefinition = "VARCHAR(255)")
    private RequestStatus requestStatus;

    private String note;

    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    @Column(name = "shipping_fee")
    private Double shippingFee = 0.0;

    @Column(name = "total_deposit")
    private Double totalDeposit = 0.0;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "distance")
    private Double distance = 0.0;

    @OneToMany(mappedBy = "borrowRequest", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<BorrowRequestItem> borrowRequestItems;
}
