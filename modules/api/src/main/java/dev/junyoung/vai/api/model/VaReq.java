package dev.junyoung.vai.api.model;

import java.util.List;

public record VaReq(
	Long id,
	String orgCd,
	String clientKey,
	int qty,
	ReqType type,
	ReqStatus status,
	List<String> bankCodes
) {
	public static VaReq newGeneral(String orgCd, String clientKey, List<String> bankCodes) {
		return new VaReq(
			null,
			orgCd,
			clientKey,
			1,
			ReqType.GENERAL,
			ReqStatus.PENDING,
			bankCodes
		);
	}

	public static VaReq newBulk(String orgCd, String clientKey, int qty, List<String> bankCodes) {
		return new VaReq(
			null,
			orgCd,
			clientKey,
			qty,
			ReqType.BULK,
			ReqStatus.PENDING,
			bankCodes
		);
	}
}
