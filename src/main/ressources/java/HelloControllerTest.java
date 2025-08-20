package com.example.springbootapp;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HelloControllerTest {

    @Test
    void testAddition() {
        int a = 2;
        int b = 3;
        int result = a + b;
        assertThat(result).isEqualTo(5);
    }
}
