package com.librarymanagementsystem.model.book.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private String title;
    private String description;
    private String isbn;
    private MultipartFile imageFile;
    private LocalDateTime publishYear;
    private Integer totalCopies;
    private Long categoryId;
    private Long authorId;
}
