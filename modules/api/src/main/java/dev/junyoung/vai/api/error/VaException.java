package dev.junyoung.vai.api.error;

public class VaException extends RuntimeException {
	private final ErrorCode errorCode;

	public VaException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}
}
