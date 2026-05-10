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

    private LocalDate publishYear;

    @NotNull(message = "Tổng số bản sao không được để trống")
    @Positive(message = "Tổng số bản sao phải lớn hơn 0")
    private Integer totalCopies;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    @NotNull(message = "Tác giả không được để trống")
    private Long authorId;
}
