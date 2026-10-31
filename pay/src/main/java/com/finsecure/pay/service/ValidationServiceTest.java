package com.finsecure.pay.service;

import com.finsecure.pay.model.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    private TransactionRequest validRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setTransactionId("TXN-001");
        req.setAmount(500.00);
        req.setCurrency("USD");
        req.setSenderAccount("ACC-123");
        req.setReceiverAccount("ACC-456");
        return req;
    }

    @Test
    void testValidTransaction() {
        ValidationService.ValidationResult result = validationService.validate(validRequest());
        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void testMissingTransactionId() {
        TransactionRequest req = validRequest();
        req.setTransactionId("");
        ValidationService.ValidationResult result = validationService.validate(req);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Transaction ID")));
    }

    @Test
    void testNegativeAmount() {
        TransactionRequest req = validRequest();
        req.setAmount(-100.00);
        ValidationService.ValidationResult result = validationService.validate(req);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("greater than zero")));
    }

    @Test
    void testAmountExceedsLimit() {
        TransactionRequest req = validRequest();
        req.setAmount(200000.00);
        ValidationService.ValidationResult result = validationService.validate(req);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("maximum limit")));
    }

    @Test
    void testUnsupportedCurrency() {
        TransactionRequest req = validRequest();
        req.setCurrency("XYZ");
        ValidationService.ValidationResult result = validationService.validate(req);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Unsupported currency")));
    }

    @Test
    void testSameAccountSenderReceiver() {
        TransactionRequest req = validRequest();
        req.setReceiverAccount("ACC-123");
        ValidationService.ValidationResult result = validationService.validate(req);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("cannot be the same")));
    }

    @Test
    void testMissingSenderAccount() {
        TransactionRequest req = validRequest();
        req.setSenderAccount("");
        ValidationService.ValidationResult result = validationService.validate(req);
        assertFalse(result.valid());
    }
}
