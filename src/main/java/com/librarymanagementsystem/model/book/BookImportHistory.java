package com.librarymanagementsystem.model.book;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

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

    private Double importPrice;

    private Double totalPrice;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime importDate;
}
