package dev.junyoung.vai.api.error;

public enum ErrorCode {
	AVAILABLE_ACCOUNT_SHORTAGE(409, "사용 가능한 가상계좌가 부족합니다");

	private final int status;
	private final String display;

	ErrorCode(int status, String display) {
		this.status = status;
		this.display = display;
	}

	public int status() {
		return status;
	}

	public String display() {
		return display;
	}
}
