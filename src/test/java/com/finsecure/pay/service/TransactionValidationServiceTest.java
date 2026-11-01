package com.finsecure.pay.service;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidationServiceTest {

    private TransactionValidationService service;

    @BeforeEach
    void setUp() {
        service = new TransactionValidationService();
        ReflectionTestUtils.setField(service, "maxAmount", 10000.0);
        ReflectionTestUtils.setField(service, "minAmount", 0.01);
        ReflectionTestUtils.setField(service, "supportedCurrenciesConfig",
            "SGD,USD,EUR,GBP");
    }

    // Helper to build a valid request
    private TransactionRequest validRequest() {
        TransactionRequest r = new TransactionRequest();
        r.setTransactionId("TXN-001");
        r.setAmount(500.00);
        r.setCurrency("SGD");
        r.setAccountFrom("ACC-001");
        r.setAccountTo("ACC-002");
        r.setDescription("Test payment");
        return r;
    }

    @Test
    @DisplayName("Valid transaction should pass all rules")
    void testValidTransaction() {
        ValidationResponse response = service.validate(validRequest());
        assertTrue(response.isValid());
        assertEquals("Transaction is valid.", response.getMessage());
    }

    @Test
    @DisplayName("Null transaction ID should fail")
    void testNullTransactionId() {
        TransactionRequest r = validRequest();
        r.setTransactionId(null);
        ValidationResponse response = service.validate(r);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("Transaction ID"));
    }

    @Test
    @DisplayName("Blank transaction ID should fail")
    void testBlankTransactionId() {
        TransactionRequest r = validRequest();
        r.setTransactionId("   ");
        assertFalse(service.validate(r).isValid());
    }

    @Test
    @DisplayName("Amount below minimum should fail")
    void testAmountBelowMinimum() {
        TransactionRequest r = validRequest();
        r.setAmount(0.001);
        ValidationResponse response = service.validate(r);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("minimum"));
    }

    @Test
    @DisplayName("Amount at minimum boundary should pass")
    void testAmountAtMinimumBoundary() {
        TransactionRequest r = validRequest();
        r.setAmount(0.01);
        assertTrue(service.validate(r).isValid());
    }

    @Test
    @DisplayName("Amount exceeding maximum should fail")
    void testAmountExceedsMaximum() {
        TransactionRequest r = validRequest();
        r.setAmount(10001.00);
        ValidationResponse response = service.validate(r);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("maximum"));
    }

    @Test
    @DisplayName("Amount at maximum boundary should pass")
    void testAmountAtMaximumBoundary() {
        TransactionRequest r = validRequest();
        r.setAmount(10000.00);
        assertTrue(service.validate(r).isValid());
    }

    @Test
    @DisplayName("Unsupported currency should fail")
    void testUnsupportedCurrency() {
        TransactionRequest r = validRequest();
        r.setCurrency("JPY");
        ValidationResponse response = service.validate(r);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("not supported"));
    }

    @Test
    @DisplayName("Null currency should fail")
    void testNullCurrency() {
        TransactionRequest r = validRequest();
        r.setCurrency(null);
        assertFalse(service.validate(r).isValid());
    }

    @Test
    @DisplayName("All supported currencies should pass")
    void testAllSupportedCurrencies() {
        for (String currency : new String[]{"SGD", "USD", "EUR", "GBP"}) {
            TransactionRequest r = validRequest();
            r.setCurrency(currency);
            assertTrue(service.validate(r).isValid(),
                "Currency " + currency + " should be valid");
        }
    }

    @Test
    @DisplayName("Blank source account should fail")
    void testBlankSourceAccount() {
        TransactionRequest r = validRequest();
        r.setAccountFrom("");
        assertFalse(service.validate(r).isValid());
    }

    @Test
    @DisplayName("Blank destination account should fail")
    void testBlankDestinationAccount() {
        TransactionRequest r = validRequest();
        r.setAccountTo("");
        assertFalse(service.validate(r).isValid());
    }

    @Test
    @DisplayName("Same source and destination should fail")
    void testSameSourceAndDestination() {
        TransactionRequest r = validRequest();
        r.setAccountTo("ACC-001");
        ValidationResponse response = service.validate(r);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("cannot be the same"));
    }

    @Test
    @DisplayName("Null amount should fail")
    void testNullAmount() {
        TransactionRequest r = validRequest();
        r.setAmount(null);
        assertFalse(service.validate(r).isValid());
    }

    

}