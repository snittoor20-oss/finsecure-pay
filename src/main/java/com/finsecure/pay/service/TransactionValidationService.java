package com.finsecure.pay.service;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TransactionValidationService {

    private static final Logger logger =
        LoggerFactory.getLogger(TransactionValidationService.class);

    @Value("${app.max.amount}")
    private double maxAmount;

    @Value("${app.min.amount}")
    private double minAmount;

    @Value("${app.supported.currencies}")
    private String supportedCurrenciesConfig;

    public ValidationResponse validate(TransactionRequest request) {
        logger.debug("Validating transaction: {}", request.getTransactionId());

        // Rule 1: Transaction ID must not be blank
        if (request.getTransactionId() == null
                || request.getTransactionId().isBlank()) {
            logger.warn("Validation failed: missing transaction ID");
            return new ValidationResponse(
                false, "Transaction ID is required.", null);
        }

        // Rule 2: Amount must not be null
        if (request.getAmount() == null) {
            logger.warn("Validation failed: null amount for txn {}",
                request.getTransactionId());
            return new ValidationResponse(
                false, "Amount is required.", request.getTransactionId());
        }

        // Rule 3: Amount must be above minimum
        if (request.getAmount() < minAmount) {
            logger.warn("Validation failed: amount {} below minimum for txn {}",
                request.getAmount(), request.getTransactionId());
            return new ValidationResponse(
                false,
                "Amount " + request.getAmount()
                    + " is below the minimum allowed of " + minAmount + ".",
                request.getTransactionId());
        }

        // Rule 4: Amount must not exceed maximum
        if (request.getAmount() > maxAmount) {
            logger.warn("Validation failed: amount {} exceeds maximum for txn {}",
                request.getAmount(), request.getTransactionId());
            return new ValidationResponse(
                false,
                "Amount " + request.getAmount()
                    + " exceeds the maximum allowed of " + maxAmount + ".",
                request.getTransactionId());
        }

        // Rule 5: Currency must be supported
        List<String> supportedCurrencies =
            Arrays.asList(supportedCurrenciesConfig.split(","));
        if (request.getCurrency() == null
                || !supportedCurrencies.contains(
                    request.getCurrency().toUpperCase())) {
            logger.warn("Validation failed: unsupported currency {} for txn {}",
                request.getCurrency(), request.getTransactionId());
            return new ValidationResponse(
                false,
                "Currency '" + request.getCurrency()
                    + "' is not supported. Supported: "
                    + supportedCurrenciesConfig,
                request.getTransactionId());
        }

        // Rule 6: Source account must not be blank
        if (request.getAccountFrom() == null
                || request.getAccountFrom().isBlank()) {
            return new ValidationResponse(
                false,
                "Source account (accountFrom) is required.",
                request.getTransactionId());
        }

        // Rule 7: Destination account must not be blank
        if (request.getAccountTo() == null
                || request.getAccountTo().isBlank()) {
            return new ValidationResponse(
                false,
                "Destination account (accountTo) is required.",
                request.getTransactionId());
        }

        // Rule 8: Source and destination must differ
        if (request.getAccountFrom().equalsIgnoreCase(request.getAccountTo())) {
            return new ValidationResponse(
                false,
                "Source and destination accounts cannot be the same.",
                request.getTransactionId());
        }

        logger.info("Transaction {} validated successfully", request.getTransactionId());
        return new ValidationResponse(
            true, "Transaction is valid.", request.getTransactionId());
    }
}