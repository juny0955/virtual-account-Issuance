package dev.junyoung.vai.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.junyoung.vai.api.controller.dto.IssueBulkVaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
@Slf4j
public class BulkVaController {

	@PostMapping
	public ResponseEntity<?> issue(@RequestBody IssueBulkVaRequest request) {
		log.info("대량 발급 요청 {}", request.toString());
		return ResponseEntity.accepted().build();
	}
}
