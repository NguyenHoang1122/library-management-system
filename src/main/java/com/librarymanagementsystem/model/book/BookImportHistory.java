package com.librarymanagementsystem.model.book;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "book_import_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    private Integer importQuantity;

    @Column(name = "import_price", precision = 15, scale = 2)
    @Max(value = 100000000, message = "Giá nhập không được vượt quá 100 triệu VNĐ")
    @Min(value = 0, message = "Giá nhập không được là số âm")
    private BigDecimal importPrice = BigDecimal.ZERO;

    @Column(name = "total_price", precision = 15, scale = 2)
    @Max(value = 1000000000, message = "Tổng tiền không được vượt quá 1 tỷ VNĐ")
    @Min(value = 0, message = "Tổng tiền không được là số âm")
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime importDate;
}
