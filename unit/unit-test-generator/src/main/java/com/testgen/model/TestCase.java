package com.testgen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String className;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String generatedTests;

    @Column(length = 50)
    private String language;

    @Column(length = 50)
    private String framework;

    @Column(length = 30)
    private String testStyle;

    // Username of the logged-in user who generated this test case
    @Column(nullable = false, length = 100)
    private String ownerUsername;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
