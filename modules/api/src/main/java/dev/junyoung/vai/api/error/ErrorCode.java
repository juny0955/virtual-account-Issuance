package dev.junyoung.vai.api.error;

public enum ErrorCode {
	IDEMPOTENCY_KEY_CONFLICT(409, "이미 사용된 멱등키의 요청 내용이 다릅니다"),
	AVAILABLE_ACCOUNT_SHORTAGE(409, "사용 가능한 가상계좌가 부족합니다"),
	INVALID_REQUEST(400, "잘못된 발급 요청입니다");

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
