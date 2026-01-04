package com.hospitalfinder.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/info")
    public String home() {
        return "Hospico API is running! Access the frontend at http://localhost:5173";
    }
}
