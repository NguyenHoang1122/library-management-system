package com.librarymanagementsystem.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "admin_wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminWalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    @Max(value = 1000000000, message = "Số tiền giao dịch không được vượt quá 1 tỷ VNĐ")
    @Min(value = -1000000000, message = "Số tiền giao dịch không được âm quá 1 tỷ VNĐ")
    private BigDecimal amount; // Số tiền giao dịch: Dương nếu là Thu, Âm nếu là Chi

    @Column(nullable = false, precision = 15, scale = 2)
    @Max(value = 1000000000, message = "Số dư sau giao dịch không được vượt quá 1 tỷ VNĐ")
    @Min(value = 0, message = "Số dư không được nhỏ hơn 0")
    private BigDecimal balanceAfter; // Số dư ví ảo sau khi thực hiện giao dịch này

    @Column(nullable = false)
    private String type; // Loại hoạt động: IMPORT (Nhập truyện), BORROW_INCOME (Thu mượn truyện), REFUND_EXPENSE (Hoàn cọc), FINE_INCOME (Phạt), MANUAL_ADJUST (Điều chỉnh)

    @Column(length = 500)
    private String description; // Mô tả chi tiết giao dịch

    @Column(nullable = false)
    private LocalDateTime transactionDate; // Ngày giờ thực hiện giao dịch

    private Long referenceId; // ID tham chiếu (Mã yêu cầu mượn truyện hoặc Mã lịch sử nhập truyện)
}
