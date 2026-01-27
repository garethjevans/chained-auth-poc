package com.example.chained.auth.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.github.client-id=test-client-id",
    "spring.security.oauth2.client.registration.github.client-secret=test-client-secret"
})
class AuthAdapterApplicationTests {

    @Test
    void contextLoads() {
    }
}
