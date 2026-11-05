package dev.junyoung.vai.api.model;

public record VaGroup(Long reqId, int issueNo) {
	public static VaGroup newGeneral(Long reqId) {
		return new VaGroup(reqId, 1);
	}
}
