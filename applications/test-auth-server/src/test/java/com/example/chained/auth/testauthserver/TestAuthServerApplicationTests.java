package com.example.chained.auth.testauthserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "server.port=0" // Use random port for tests
    })
class TestAuthServerApplicationTests {

  @Test
  void contextLoads() {
    // Test that the Spring context loads successfully
  }
}
