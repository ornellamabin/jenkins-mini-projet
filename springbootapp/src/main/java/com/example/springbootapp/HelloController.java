package com.example.springbootapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HelloController {

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hello Jenkins CI/CD!");
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Application is running successfully!");
    }

    @GetMapping("/greet")
    public ResponseEntity<String> greetUser(@RequestParam(required = false) String name) {
        String userName = (name != null) ? name : "Guest";
        return ResponseEntity.ok(String.format("Hello %s! Welcome to our CI/CD pipeline!", userName));
    }
}
