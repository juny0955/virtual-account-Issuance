package dev.junyoung.vai.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
	@ExceptionHandler(VaException.class)
	public ProblemDetail handleVaException(VaException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(exception.errorCode().status()), exception.getMessage());
		problem.setProperty("code", exception.errorCode().name());
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpectedException(Exception exception) {
		log.error("처리되지 않은 오류가 발생했습니다", exception);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");
	}
}
