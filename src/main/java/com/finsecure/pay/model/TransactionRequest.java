package com.finsecure.pay.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private String transactionId;
    private Double amount;
    private String currency;
    private String accountFrom;
    private String accountTo;
    private String description;
}