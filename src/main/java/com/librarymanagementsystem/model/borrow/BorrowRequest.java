package com.librarymanagementsystem.model.borrow;

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
    private RequestStatus requestStatus;

    private String note;

    private String rejectionReason;

    @OneToMany(mappedBy = "borrowRequest", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<BorrowRequestItem> borrowRequestItems;
}
