package com.testgen.service;

import com.testgen.dto.GenerateRequest;
import com.testgen.dto.GenerateResponse;
import com.testgen.model.TestCase;
import com.testgen.repository.TestCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TestCaseService {

    private final GroqService groqService;
    private final TestCaseRepository repository;

    // Loosely detects "class Foo" or "public class Foo" or "def foo" style names
    private static final Pattern JAVA_CLASS_PATTERN =
            Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
    private static final Pattern PYTHON_DEF_PATTERN =
            Pattern.compile("def\\s+(\\w+)");

    public TestCaseService(GroqService groqService, TestCaseRepository repository) {
        this.groqService = groqService;
        this.repository = repository;
    }

    public GenerateResponse generateAndSave(GenerateRequest request, String ownerUsername) {
        String className = (request.getClassName() != null && !request.getClassName().isBlank())
                ? request.getClassName()
                : inferClassName(request.getSourceCode());

        String generatedTests;
        try {
            generatedTests = groqService.generateUnitTests(
                    request.getSourceCode(), request.getLanguage(), request.getFramework(), request.getTestStyle());
        } catch (Exception e) {
            log.error("Test generation failed", e);
            return GenerateResponse.ofError("Generation failed: " + e.getMessage());
        }

        TestCase entity = new TestCase();
        entity.setClassName(className);
        entity.setSourceCode(request.getSourceCode());
        entity.setGeneratedTests(generatedTests);
        entity.setLanguage(request.getLanguage());
        entity.setFramework(request.getFramework());
        entity.setTestStyle(request.getTestStyle());
        entity.setOwnerUsername(ownerUsername);

        TestCase saved = repository.save(entity);

        return new GenerateResponse(
                saved.getId(),
                saved.getClassName(),
                saved.getGeneratedTests(),
                saved.getLanguage(),
                saved.getFramework(),
                saved.getTestStyle(),
                true,
                null
        );
    }

    public List<TestCase> getAllHistory(String ownerUsername) {
        return repository.findByOwnerUsernameOrderByCreatedAtDesc(ownerUsername);
    }

    public List<TestCase> search(String ownerUsername, String query) {
        return repository.findByOwnerUsernameAndClassNameContainingIgnoreCaseOrderByCreatedAtDesc(
                ownerUsername, query);
    }

    public TestCase getById(Long id, String ownerUsername) {
        return repository.findByIdAndOwnerUsername(id, ownerUsername)
                .orElseThrow(() -> new RuntimeException("Test case not found with id: " + id));
    }

    public void delete(Long id, String ownerUsername) {
        repository.deleteByIdAndOwnerUsername(id, ownerUsername);
    }

    private String inferClassName(String sourceCode) {
        Matcher javaMatcher = JAVA_CLASS_PATTERN.matcher(sourceCode);
        if (javaMatcher.find()) {
            return javaMatcher.group(1);
        }
        Matcher pyMatcher = PYTHON_DEF_PATTERN.matcher(sourceCode);
        if (pyMatcher.find()) {
            return pyMatcher.group(1);
        }
        return "UnnamedClass";
    }
}
