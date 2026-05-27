package com.librarymanagementsystem.model.borrow.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CombinedHistoryDTO {
    private Long requestId;
    private String booksSummary;
    private String requestDate;
    private String returnDate;
    private String status;
    private BigDecimal totalDeposit;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private Long transactionId;
}
