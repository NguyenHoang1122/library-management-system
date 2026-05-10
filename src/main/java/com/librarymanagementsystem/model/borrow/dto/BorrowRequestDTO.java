package com.librarymanagementsystem.model.borrow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequestDTO {
    private Long id;
    private Long userId;
    private LocalDateTime requestDate;
    private String status;
    private List<Long> bookIds;
}
