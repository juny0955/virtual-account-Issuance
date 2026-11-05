package dev.junyoung.vai.api.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.junyoung.vai.api.error.GlobalExceptionHandler;
import dev.junyoung.vai.api.repository.VaReqRepository;
import dev.junyoung.vai.api.service.BulkVaService;

class BulkVaControllerTest {
    private final VaReqRepository repository = mock(VaReqRepository.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        when(repository.save(any())).thenReturn(new VaReqRepository.SaveResult(1L, true));
        when(repository.orgExists("T0001")).thenReturn(true);
        when(repository.banksExist(List.of("902", "901"))).thenReturn(true);
        mvc = MockMvcBuilders.standaloneSetup(new BulkVaController(new BulkVaService(repository)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void acceptsValidRequestWithoutBody() throws Exception {
        mvc.perform(post("/api/v1/bulk").header("Idempotency-Key", "test-key").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"orgCd":"T0001","bankCodes":["902","901"],"qty":200000}
                    """))
            .andExpect(status().isAccepted()).andExpect(content().string(""));
        verify(repository).save(argThat(req -> req.type().name().equals("BULK")
            && req.status().name().equals("PENDING") && req.clientKey().equals("test-key") && req.qty() == 200000
            && req.bankCodes().equals(List.of("902", "901"))));
    }

    @Test
    void rejectsInvalidRequestsBeforeCallingService() throws Exception {
        for (String body : List.of(
            "{}",
            "{\"orgCd\":\" \",\"bankCodes\":[\"901\"],\"qty\":1}",
            "{\"orgCd\":\"TOOLONG\",\"bankCodes\":[\"901\"],\"qty\":1}",
            "{\"orgCd\":\"T0001\",\"bankCodes\":[],\"qty\":1}",
            "{\"orgCd\":\"T0001\",\"bankCodes\":[null],\"qty\":1}",
            "{\"orgCd\":\"T0001\",\"bankCodes\":[\"901\",\"901\"],\"qty\":1}",
            "{\"orgCd\":\"T0001\",\"bankCodes\":[\"901\"],\"qty\":0}",
            "{\"orgCd\":\"T0001\",\"bankCodes\":[\"901\"],\"qty\":-1}",
            "{\"orgCd\":\"T0001\",\"bankCodes\":[" + "\"901\",".repeat(20) + "\"902\"],\"qty\":1}")) {
            mvc.perform(post("/api/v1/bulk").header("Idempotency-Key", "test-key").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnknownOrganizationOrBank() throws Exception {
        for (String body : List.of(
            "{\"orgCd\":\"T9999\",\"bankCodes\":[\"901\"],\"qty\":1}",
            "{\"orgCd\":\"T0001\",\"bankCodes\":[\"999\"],\"qty\":1}")) {
            mvc.perform(post("/api/v1/bulk").header("Idempotency-Key", "test-key").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        verify(repository, never()).save(any());
    }


    @Test
    void rejectsInvalidClientKeys() throws Exception {
        String body = "{\"orgCd\":\"T0001\",\"bankCodes\":[\"902\",\"901\"],\"qty\":1}";
        mvc.perform(post("/api/v1/bulk").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        for (String key : List.of("", "   ", "x".repeat(101))) {
            mvc.perform(post("/api/v1/bulk").header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(repository);
    }
}
