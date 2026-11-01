package com.finsecure.pay.controller;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResponse;
import com.finsecure.pay.service.TransactionValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class TransactionController {

    private static final Logger logger =
        LoggerFactory.getLogger(TransactionController.class);

    private final TransactionValidationService validationService;

    public TransactionController(TransactionValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(
            @RequestBody TransactionRequest request) {
        logger.info("Received validation request for txn: {}",
            request.getTransactionId());
        ValidationResponse response = validationService.validate(request);
        int httpStatus = response.isValid() ? 200 : 400;
        return ResponseEntity.status(httpStatus).body(response);
    }

    @GetMapping("/validate/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("FinSecure Pay is running");
    }
}