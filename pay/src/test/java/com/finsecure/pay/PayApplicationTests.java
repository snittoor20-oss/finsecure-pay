package com.finsecure.pay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PayApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void demonstratePipelineFailure() {
        Assertions.fail(
            "Intentional test failure to demonstrate Jenkins deployment blocking"
        );
    }
}