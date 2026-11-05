package dev.junyoung.vai.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.junyoung.vai.api.error.ErrorCode;
import dev.junyoung.vai.api.error.VaException;
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
    public ResponseEntity<IssueResult> issue(@RequestHeader(value = "Idempotency-Key", required = false) String clientKey,
		@Valid @RequestBody IssueGeneralVaRequest request) {
		if (clientKey == null || clientKey.isBlank() || clientKey.length() > 100) {
			throw new VaException(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.display());
		}
        return ResponseEntity.ok(service.issue(request, clientKey));
    }
}
