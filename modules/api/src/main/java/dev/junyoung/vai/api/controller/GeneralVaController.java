package dev.junyoung.vai.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.junyoung.vai.api.controller.dto.IssueGeneralVaRequest;
import dev.junyoung.vai.api.service.GeneralVaService;
import dev.junyoung.vai.api.service.result.IssueResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/general")
@RequiredArgsConstructor
public class GeneralVaController {
    private final GeneralVaService service;

    @PostMapping
    public ResponseEntity<IssueResult> issue(@Valid @RequestBody IssueGeneralVaRequest request) {
        return ResponseEntity.ok(service.issue(request));
    }
}
