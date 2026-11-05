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
import dev.junyoung.vai.api.controller.dto.IssueBulkVaRequest;
import lombok.RequiredArgsConstructor;
import dev.junyoung.vai.api.service.BulkVaService;

@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
public class BulkVaController {

	private final BulkVaService service;

	@PostMapping
	public ResponseEntity<Void> issue(@RequestHeader(value = "Idempotency-Key", required = false) String clientKey,
		@Valid @RequestBody IssueBulkVaRequest request) {
		if (clientKey == null || clientKey.isBlank() || clientKey.length() > 100) {
			throw new VaException(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.display());
		}
		service.issue(request, clientKey);
		return ResponseEntity.accepted().build();
	}
}
