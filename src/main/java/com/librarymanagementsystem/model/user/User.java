package com.librarymanagementsystem.model.user;

import com.librarymanagementsystem.model.Notification;
import com.librarymanagementsystem.model.book.Wishlist;
import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.status.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET delete_at = CURRENT_TIMESTAMP WHERE id = ?")
//@Where(clause = "delete_at IS NULL")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String userName;
    private String password;
    private String email;
    private String phoneNumber;
    private String address;

    @Column(length = 500)
    private String image;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Max(value = 1000000000, message = "Số dư quỹ không được vượt quá 1 tỷ VNĐ")
    @Min(value = 0, message = "Số dư quỹ không được nhỏ hơn 0")
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime updateDate;

    @Column(name = "delete_at")
    private LocalDateTime deleteAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "user")
    private List<Wishlist> wishlists;

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "user")
    private List<BorrowRequest> borrowRequests;

    @OneToMany(mappedBy = "user")
    private List<BorrowTransaction> borrowTransactions;
}
