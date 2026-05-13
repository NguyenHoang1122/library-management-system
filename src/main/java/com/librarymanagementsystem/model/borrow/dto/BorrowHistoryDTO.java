package com.librarymanagementsystem.model.borrow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowHistoryDTO {
   private Long id;
   private String bookName;
   private String borrowDate;
   private String dueDate;
   private String returnDate;
   private String status;
}
