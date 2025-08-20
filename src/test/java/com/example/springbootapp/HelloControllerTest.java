package com.example.springbootapp;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HelloControllerTest {

    @Test
    void testSayHello() {
        HelloController controller = new HelloController();
        String response = controller.sayHello();
        assertThat(response).isEqualTo("Hello Jenkins CI/CD!");
    }
}
