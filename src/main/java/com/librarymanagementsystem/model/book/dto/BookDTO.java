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

    @NotNull(message = "Danh mục không được để trống")
    private List<Long> categoryIds;

    @NotBlank(message = "Tên tác giả không được để trống")
    private String authorName;
}
