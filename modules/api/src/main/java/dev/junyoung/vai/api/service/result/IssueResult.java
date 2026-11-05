package dev.junyoung.vai.api.service.result;

import java.util.List;
import java.util.Map;

public record IssueResult(Long requestId, List<Account> accounts) {
	public static IssueResult of(Long requestId, List<String> bankCodes, Map<String, String> accounts) {
		return new IssueResult(requestId, bankCodes.stream()
			.map(bankCode -> new Account(bankCode, accounts.get(bankCode))).toList());
	}

	public record Account(String bankCode, String vaNo) {}
}
