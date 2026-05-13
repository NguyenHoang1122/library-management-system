package com.librarymanagementsystem.model.borrow;

import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


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
    @Column(name = "request_status")
    private RequestStatus requestStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;  // Lý do từ chối từ librarian
}