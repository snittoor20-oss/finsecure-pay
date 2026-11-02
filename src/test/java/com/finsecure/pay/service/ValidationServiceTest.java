package com.finsecure.pay.service;

import com.finsecure.pay.model.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationService Unit Tests")
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // ── Helper method to build a fully valid request ──────────────────────
    private TransactionRequest validRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setTransactionId("TXN-001");
        req.setAmount(500.00);
        req.setCurrency("USD");
        req.setAccountFrom("ACC-123");
        req.setAccountTo("ACC-456");
        return req;
    }

    // ── PASSING TEST CASES ─────────────────────────────────────────────────

    @Test
    @DisplayName("TC01 - Valid transaction should pass validation")
    void testValidTransaction() {
        ValidationService.ValidationResult result =
                validationService.validate(validRequest());

        assertTrue(result.valid(), "Expected transaction to be valid");
        assertEquals("Transaction is valid", result.message());
        assertTrue(result.errors().isEmpty(), "Expected no validation errors");
    }

    @Test
    @DisplayName("TC02 - Valid transaction with EUR currency should pass")
    void testValidTransactionWithEurCurrency() {
        TransactionRequest req = validRequest();
        req.setCurrency("EUR");

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("TC03 - Valid transaction with SGD currency should pass")
    void testValidTransactionWithSgdCurrency() {
        TransactionRequest req = validRequest();
        req.setCurrency("SGD");

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertTrue(result.valid());
    }

    @Test
    @DisplayName("TC04 - Minimum valid amount (0.01) should pass")
    void testMinimumValidAmount() {
        TransactionRequest req = validRequest();
        req.setAmount(0.01);

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertTrue(result.valid());
    }

    @Test
    @DisplayName("TC05 - Maximum valid amount (100000.00) should pass")
    void testMaximumValidAmount() {
        TransactionRequest req = validRequest();
        req.setAmount(100000.00);

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertTrue(result.valid());
    }

    // ── FAILING TEST CASES ─────────────────────────────────────────────────

    @Test
    @DisplayName("TC06 - Missing transaction ID should fail validation")
    void testMissingTransactionId() {
        TransactionRequest req = validRequest();
        req.setTransactionId("");

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid(), "Expected validation to fail");
        assertTrue(
            result.errors().stream().anyMatch(e -> e.contains("Transaction ID")),
            "Expected error about Transaction ID"
        );
    }

    @Test
    @DisplayName("TC07 - Null transaction ID should fail validation")
    void testNullTransactionId() {
        TransactionRequest req = validRequest();
        req.setTransactionId(null);

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Transaction ID")));
    }

    @Test
    @DisplayName("TC08 - Zero amount should fail validation")
    void testZeroAmount() {
        TransactionRequest req = validRequest();
        req.setAmount(0.00);

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(
            result.errors().stream().anyMatch(e -> e.contains("greater than zero")),
            "Expected error about amount being greater than zero"
        );
    }

    @Test
    @DisplayName("TC09 - Negative amount should fail validation")
    void testNegativeAmount() {
        TransactionRequest req = validRequest();
        req.setAmount(-100.00);

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("greater than zero")));
    }

    @Test
    @DisplayName("TC10 - Amount exceeding maximum limit should fail validation")
    void testAmountExceedsLimit() {
        TransactionRequest req = validRequest();
        req.setAmount(200000.00);

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(
            result.errors().stream().anyMatch(e -> e.contains("maximum limit")),
            "Expected error about maximum limit"
        );
    }

    @Test
    @DisplayName("TC11 - Unsupported currency should fail validation")
    void testUnsupportedCurrency() {
        TransactionRequest req = validRequest();
        req.setCurrency("XYZ");

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(
            result.errors().stream().anyMatch(e -> e.contains("Unsupported currency")),
            "Expected error about unsupported currency"
        );
    }

    @Test
    @DisplayName("TC12 - Null currency should fail validation")
    void testNullCurrency() {
        TransactionRequest req = validRequest();
        req.setCurrency(null);

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Unsupported currency")));
    }

    @Test
    @DisplayName("TC13 - Missing sender account should fail validation")
    void testMissingSenderAccount() {
        TransactionRequest req = validRequest();
        req.setAccountFrom("");

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(
            result.errors().stream().anyMatch(e -> e.contains("Sender account")),
            "Expected error about sender account"
        );
    }

    @Test
    @DisplayName("TC14 - Missing receiver account should fail validation")
    void testMissingReceiverAccount() {
        TransactionRequest req = validRequest();
        req.setAccountTo("");

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(
            result.errors().stream().anyMatch(e -> e.contains("Receiver account")),
            "Expected error about receiver account"
        );
    }

    @Test
    @DisplayName("TC15 - Same sender and receiver account should fail validation")
    void testSameAccountSenderReceiver() {
        TransactionRequest req = validRequest();
        req.setAccountTo("ACC-123"); // same as sender

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(
            result.errors().stream().anyMatch(e -> e.contains("cannot be the same")),
            "Expected error about same sender and receiver"
        );
    }

    @Test
    @DisplayName("TC16 - Multiple validation errors should all be reported")
    void testMultipleValidationErrors() {
        TransactionRequest req = new TransactionRequest();
        req.setTransactionId("");
        req.setAmount(-50.00);
        req.setCurrency("INVALID");
        req.setAccountFrom("");
        req.setAccountTo("");

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertFalse(result.valid());
        assertTrue(result.errors().size() >= 4,
            "Expected at least 4 validation errors, got: " + result.errors().size());
    }

    @Test
    @DisplayName("TC17 - Lowercase currency code should be accepted")
    void testLowercaseCurrencyCode() {
        TransactionRequest req = validRequest();
        req.setCurrency("usd"); // lowercase

        ValidationService.ValidationResult result =
                validationService.validate(req);

        assertTrue(result.valid(),
            "Lowercase currency 'usd' should be treated as valid USD");
    }


}