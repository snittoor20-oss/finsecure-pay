package com.finsecure.pay.controller;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class TransactionController {

    private final ValidationService validationService;

    public TransactionController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "FinSecure Pay",
            "version", "1.0.0"
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationService.ValidationResult> validate(
            @RequestBody TransactionRequest request) {
        ValidationService.ValidationResult result = validationService.validate(request);
        int statusCode = result.valid() ? 200 : 400;
        return ResponseEntity.status(statusCode).body(result);
    }
}