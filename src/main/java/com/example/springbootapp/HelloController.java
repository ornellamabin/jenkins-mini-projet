package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    
    @GetMapping("/")
    public String hello() {
        return "🚀 Hello from Jenkins CI/CD Pipeline!";
    }
    
    @GetMapping("/api/test")
    public String test() {
        return "✅ API working perfectly!";
    }
}