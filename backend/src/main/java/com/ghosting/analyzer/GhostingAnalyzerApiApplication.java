package com.ghosting.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ghosting.analyzer")
public class GhostingAnalyzerApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(GhostingAnalyzerApiApplication.class, args);
    }
}
