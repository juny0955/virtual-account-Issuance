package dev.junyoung.vai.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.junyoung.vai.api.controller.dto.IssueBulkVaRequest;
import lombok.RequiredArgsConstructor;
import dev.junyoung.vai.api.service.BulkVaService;

@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
public class BulkVaController {

	private final BulkVaService service;

	@PostMapping
	public ResponseEntity<Void> issue(@Valid @RequestBody IssueBulkVaRequest request) {
		service.issue(request);
		return ResponseEntity.accepted().build();
	}
}
