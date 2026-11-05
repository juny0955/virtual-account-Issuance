package dev.junyoung.vai.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import dev.junyoung.vai.api.controller.dto.IssueBulkVaRequest;
import dev.junyoung.vai.api.repository.VaReqRepository;
import dev.junyoung.vai.api.error.VaException;
import dev.junyoung.vai.api.error.ErrorCode;

@EnabledIfEnvironmentVariable(named = "VAI_TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${VAI_TEST_DB_URL}",
    "spring.datasource.username=${VAI_TEST_DB_USER:postgres}",
    "spring.datasource.password=${VAI_TEST_DB_PASSWORD:}"
})
class BulkVaServiceTest {
    @Autowired BulkVaService service;
    @Autowired DSLContext db;
    @MockitoSpyBean VaReqRepository repository;

    @BeforeEach
    void setup() {
        db.execute("TRUNCATE va_alloc, va_bulk_result, va_group, va_req_bank, va_req, va, org CASCADE");
        db.execute("INSERT INTO org(code, name) VALUES ('T0001', 'Test')");
        db.execute("INSERT INTO bank(code, name) VALUES ('901', 'Test1'), ('902', 'Test2') ON CONFLICT (code) DO NOTHING");
    }

    @Test
    void savesPendingRequestAndOrderedBanksWithoutAllocations() {
        service.issue(new IssueBulkVaRequest("T0001", List.of("902", "901"), 200000), "test-key");
        var req = db.fetchOne("SELECT id, type, status, qty FROM va_req");
        assertNotNull(req);
        assertEquals("BULK", req.get("type"));
        assertEquals("PENDING", req.get("status"));
        assertEquals(200000, req.get("qty"));
        assertEquals(List.of("902", "901"), db.fetch("SELECT bank_cd FROM va_req_bank WHERE req_id = ? ORDER BY bank_order", req.get("id")).getValues(0, String.class));
        assertEquals(0, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_ALLOC));
        assertEquals(0, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_BULK_RESULT));
    }

    @Test
    void rejectsUnknownOrganizationOrBankWithoutSaving() {
        assertThrows(VaException.class, () -> service.issue(new IssueBulkVaRequest("T9999", List.of("901"), 1), "test-key"));
        assertThrows(VaException.class, () -> service.issue(new IssueBulkVaRequest("T0001", List.of("901", "999"), 1), "test-key"));
        assertEquals(0, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
    }

    @Test
    void rollsBackRequestAndBanksWhenSaveFails() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("simulated failure");
        }).when(repository).save(any());
        assertThrows(IllegalStateException.class, () -> service.issue(new IssueBulkVaRequest("T0001", List.of("901"), 1), "test-key"));
        assertEquals(0, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
        assertEquals(0, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ_BANK));
    }

    @Test
    void retryDoesNotCreateAnotherRequestAndChangedQuantityConflicts() {
        var request = new IssueBulkVaRequest("T0001", List.of("901"), 2);
        service.issue(request, "bulk-key");
        service.issue(request, "bulk-key");
        assertEquals("bulk-key", db.fetchValue("SELECT client_key FROM va_req"));
        var conflict = assertThrows(VaException.class, () -> service.issue(
            new IssueBulkVaRequest("T0001", List.of("901"), 3), "bulk-key"));
        assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, conflict.errorCode());
        assertEquals(1, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
    }
}
