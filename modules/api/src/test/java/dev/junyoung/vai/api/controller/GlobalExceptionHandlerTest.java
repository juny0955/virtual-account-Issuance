package dev.junyoung.vai.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.junyoung.vai.api.error.ErrorCode;
import dev.junyoung.vai.api.error.GlobalExceptionHandler;
import dev.junyoung.vai.api.error.VaException;
import dev.junyoung.vai.api.service.GeneralVaService;

class GlobalExceptionHandlerTest {
	private final GeneralVaService service = mock(GeneralVaService.class);
	private MockMvc mvc;

	@BeforeEach
	void setup() {
		mvc = MockMvcBuilders.standaloneSetup(new GeneralVaController(service))
			.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void rejectsInvalidGeneralRequestsBeforeCallingService() throws Exception {
		for (String body : java.util.List.of("{}", "{\"orgCd\":\"T0001\",\"bankCodes\":[\"901\",\"901\"]}",
			"{\"orgCd\":\"T0001\",\"bankCodes\":[\"    \"]}")) {
			mvc.perform(post("/api/v1/general").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
		verifyNoInteractions(service);
	}

	@Test
	void returnsConflictWithReason() throws Exception {
		when(service.issue(any())).thenThrow(new VaException(ErrorCode.AVAILABLE_ACCOUNT_SHORTAGE, "사용 가능한 가상계좌가 부족합니다: 901"));
		mvc.perform(post("/api/v1/general").contentType(MediaType.APPLICATION_JSON).content("{\"orgCd\":\"T0001\",\"bankCodes\":[\"901\"]}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("사용 가능한 가상계좌가 부족합니다: 901"))
			.andExpect(jsonPath("$.code").value("AVAILABLE_ACCOUNT_SHORTAGE"));
	}

	@Test
	void preservesBadRequestForInvalidJson() throws Exception {
		mvc.perform(post("/api/v1/general").contentType(MediaType.APPLICATION_JSON).content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void preservesMethodNotAllowed() throws Exception {
		mvc.perform(get("/api/v1/general"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(header().string("Allow", "POST"));
	}

	@Test
	void hidesUnexpectedExceptionDetails() throws Exception {
		when(service.issue(any())).thenThrow(new IllegalStateException("secret"));
		mvc.perform(post("/api/v1/general").contentType(MediaType.APPLICATION_JSON).content("{\"orgCd\":\"T0001\",\"bankCodes\":[\"901\"]}"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.detail").value("서버 오류가 발생했습니다"));
	}
}
