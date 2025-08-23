package com.example.springbootapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour HelloController
 */
@ExtendWith(MockitoExtension.class)
class HelloControllerTest {

    @InjectMocks
    private HelloController helloController;

    @Test
    void testSayHello() {
        // When
        ResponseEntity<String> response = helloController.sayHello();
        
        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Hello Jenkins CI/CD!");
    }

    @Test
    void testHealthCheck() {
        // When
        ResponseEntity<String> response = helloController.healthCheck();
        
        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Application is running successfully!");
    }

    @Test
    void testGreetUserWithDefault() {
        // When
        ResponseEntity<String> response = helloController.greetUser(null);
        
        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Hello Guest! Welcome to our CI/CD pipeline!");
    }

    @Test
    void testGreetUserWithName() {
        // When
        ResponseEntity<String> response = helloController.greetUser("Alice");
        
        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Hello Alice! Welcome to our CI/CD pipeline!");
    }
}