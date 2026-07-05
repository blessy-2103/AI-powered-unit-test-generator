package com.testgen.controller;

import com.testgen.dto.GenerateRequest;
import com.testgen.dto.GenerateResponse;
import com.testgen.model.TestCase;
import com.testgen.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {

    private final TestCaseService service;

    public TestCaseController(TestCaseService service) {
        this.service = service;
    }

    // POST /api/testcases/generate  -> calls Groq, saves result, returns it
    @PostMapping("/generate")
    public ResponseEntity<GenerateResponse> generate(@Valid @RequestBody GenerateRequest request,
                                                       Authentication auth) {
        GenerateResponse response = service.generateAndSave(request, auth.getName());
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // GET /api/testcases -> current user's history, most recent first
    @GetMapping
    public ResponseEntity<List<TestCase>> getAll(Authentication auth) {
        return ResponseEntity.ok(service.getAllHistory(auth.getName()));
    }

    // GET /api/testcases/search?query=Calculator
    @GetMapping("/search")
    public ResponseEntity<List<TestCase>> search(@RequestParam String query, Authentication auth) {
        return ResponseEntity.ok(service.search(auth.getName(), query));
    }

    // GET /api/testcases/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getById(id, auth.getName()));
    }

    // DELETE /api/testcases/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Deleted test case " + id));
    }
}
