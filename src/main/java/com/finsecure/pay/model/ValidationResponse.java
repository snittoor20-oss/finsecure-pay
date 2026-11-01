package com.finsecure.pay.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {
    private boolean valid;
    private String message;
    private String transactionId;
    private LocalDateTime timestamp;

    public ValidationResponse(boolean valid, String message, String transactionId) {
        this.valid = valid;
        this.message = message;
        this.transactionId = transactionId;
        this.timestamp = LocalDateTime.now();
    }
}