package dev.junyoung.vai.api.controller.dto;

import java.util.List;

public record IssueGeneralVaRequest(
	String orgCd,
	List<String> bankCodes
) {
}
