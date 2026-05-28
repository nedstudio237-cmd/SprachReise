package com.sprachreise.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
            "status", "OK",
            "message", "SprachReise API is running!",
            "version", "1.0.0"
        );
    }
}
