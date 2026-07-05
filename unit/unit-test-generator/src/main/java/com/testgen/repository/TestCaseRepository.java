package com.testgen.repository;

import com.testgen.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    // Scoped to the logged-in user so everyone sees only their own history
    List<TestCase> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);

    List<TestCase> findByOwnerUsernameAndClassNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String ownerUsername, String className);

    Optional<TestCase> findByIdAndOwnerUsername(Long id, String ownerUsername);

    void deleteByIdAndOwnerUsername(Long id, String ownerUsername);
}
