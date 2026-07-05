package com.testgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestGenApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestGenApplication.class, args);
        System.out.println("=================================================");
        System.out.println(" AI-Powered Unit Test Generator is running!");
        System.out.println(" Open http://localhost:8080 in your browser");
        System.out.println("=================================================");
    }
}
