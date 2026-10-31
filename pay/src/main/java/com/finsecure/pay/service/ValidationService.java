package com.finsecure.pay.service;

import com.finsecure.pay.model.TransactionRequest;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

    private static final double MAX_AMOUNT = 100000.00;
    private static final List<String> SUPPORTED_CURRENCIES = List.of("USD", "EUR", "SGD", "GBP");

    public ValidationResult validate(TransactionRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            errors.add("Transaction ID is required");
        }
        if (request.getAmount() <= 0) {
            errors.add("Amount must be greater than zero");
        }
        if (request.getAmount() > MAX_AMOUNT) {
            errors.add("Amount exceeds maximum limit of " + MAX_AMOUNT);
        }
        if (request.getCurrency() == null || !SUPPORTED_CURRENCIES.contains(request.getCurrency().toUpperCase())) {
            errors.add("Unsupported currency. Supported: " + SUPPORTED_CURRENCIES);
        }
        if (request.getSenderAccount() == null || request.getSenderAccount().isBlank()) {
            errors.add("Sender account is required");
        }
        if (request.getReceiverAccount() == null || request.getReceiverAccount().isBlank()) {
            errors.add("Receiver account is required");
        }
        if (request.getSenderAccount() != null &&
            request.getSenderAccount().equals(request.getReceiverAccount())) {
            errors.add("Sender and receiver accounts cannot be the same");
        }

        if (errors.isEmpty()) {
            return new ValidationResult(true, "Transaction is valid", errors);
        } else {
            return new ValidationResult(false, "Transaction validation failed", errors);
        }
    }

    public record ValidationResult(boolean valid, String message, List<String> errors) {}
}