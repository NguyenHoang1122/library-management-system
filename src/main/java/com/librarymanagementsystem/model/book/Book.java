package com.librarymanagementsystem.model.book;

import com.librarymanagementsystem.model.user.Author;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true)
    private String isbn;

    @Column(length = 500)
    private String image;

    private LocalDate publishYear;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "import_price", precision = 15, scale = 2)
    @Max(value = 100000000, message = "Giá nhập không được vượt quá 100 triệu VNĐ")
    @Min(value = 0, message = "Giá nhập không được là số âm")
    private BigDecimal importPrice = BigDecimal.ZERO;

    @Column(name = "deposit_price", precision = 15, scale = 2)
    @Max(value = 100000000, message = "Giá cọc không được vượt quá 100 triệu VNĐ")
    @Min(value = 0, message = "Giá cọc không được là số âm")
    private BigDecimal depositPrice = BigDecimal.ZERO;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;
    
    //phương pháp trung gian trả về id của danh mục.
    @Transient
    public List<Long> getCategoryIds() {
        if (categories == null) return java.util.Collections.emptyList();
        return categories.stream().map(Category::getId).toList();
    }
}
