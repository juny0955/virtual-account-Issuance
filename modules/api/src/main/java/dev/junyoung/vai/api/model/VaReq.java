package dev.junyoung.vai.api.model;

import java.util.List;

public record VaReq(
	Long id,
	String orgCd,
	int qty,
	ReqType type,
	ReqStatus status,
	List<String> bankCodes
) {
	public static VaReq newGeneral(String orgCd, List<String> bankCodes) {
		return new VaReq(
			null,
			orgCd,
			1,
			ReqType.GENERAL,
			ReqStatus.PENDING,
			bankCodes
		);
	}

	public static VaReq newBulk(String orgCd, int qty, List<String> bankCodes) {
		return new VaReq(
			null,
			orgCd,
			qty,
			ReqType.BULK,
			ReqStatus.PENDING,
			bankCodes
		);
	}
}
