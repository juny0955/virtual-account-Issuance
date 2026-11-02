package dev.junyoung.vai.api.controller.dto;

import java.util.List;

public record IssueBulkVaRequest(
	String orgCd,
	List<String> bankCodes,
	int qty
) {
}
