package com.librarymanagementsystem.model.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private Long id;

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "ISBN không được để trống")

    private String isbn;

    private MultipartFile imageFile;

    private String image;

    private LocalDate publishYear;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Danh mục không được để trống")
    private List<Long> categoryIds;

    @NotBlank(message = "Tên tác giả không được để trống")
    private String authorName;

    @NotNull(message = "Giá nhập không được để trống")
    @Positive(message = "Giá nhập phải lớn hơn 0")
    @Max(value = 100000000, message = "Giá nhập không được vượt quá 100,000,000 đ")
    private BigDecimal importPrice;

    @NotNull(message = "Giá cọc không được để trống")
    @Positive(message = "Giá cọc phải lớn hơn 0")
    @Max(value = 100000000, message = "Giá cọc không được vượt quá 100,000,000 đ")
    private BigDecimal depositPrice;
}
