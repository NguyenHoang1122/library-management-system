package com.librarymanagementsystem.model.borrow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CombinedHistoryDTO {
    private Long requestId;
    private String booksSummary;
    private String requestDate;
    private String returnDate;
    private String status;
    private Double totalDeposit;
    private Double shippingFee;
    private Double totalAmount;
    private Long transactionId;
}
