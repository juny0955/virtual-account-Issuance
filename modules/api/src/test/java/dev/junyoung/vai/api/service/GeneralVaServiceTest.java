package dev.junyoung.vai.api.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.junyoung.vai.api.controller.dto.IssueGeneralVaRequest;
import dev.junyoung.vai.api.error.ErrorCode;
import dev.junyoung.vai.api.error.VaException;
import dev.junyoung.vai.api.service.result.IssueResult;

@EnabledIfEnvironmentVariable(named = "VAI_TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${VAI_TEST_DB_URL}",
    "spring.datasource.username=${VAI_TEST_DB_USER:postgres}",
    "spring.datasource.password=${VAI_TEST_DB_PASSWORD:}"
})
class GeneralVaServiceTest {
    @Autowired GeneralVaService service;
    @Autowired DSLContext db;

    @BeforeEach
    void setup() {
        db.execute("TRUNCATE va_alloc, va_bulk_result, va_group, va_req_bank, va_req, va, org CASCADE");
        db.execute("INSERT INTO org(code, name) VALUES ('T0001', 'Test')");
        db.execute("INSERT INTO bank(code, name) VALUES ('901', 'Test1'), ('902', 'Test2') ON CONFLICT (code) DO NOTHING");
    }

    @Test
    void issuesAccountsInRequestedOrder() {
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE'), ('902', 'b', 'AVAILABLE')");
        var result = service.issue(new IssueGeneralVaRequest("T0001", List.of("902", "901")));
        assertEquals(List.of(new IssueResult.Account("902", "b"), new IssueResult.Account("901", "a")), result.accounts());
        assertEquals("COMPLETED", db.fetchValue("SELECT status FROM va_req WHERE id = ?", result.requestId()));
        assertEquals(List.of("902", "901"), db.fetch("SELECT bank_cd FROM va_req_bank WHERE req_id = ? ORDER BY bank_order", result.requestId()).getValues(0, String.class));
        assertEquals(2, db.fetchCount(db.selectFrom(dev.junyoung.vai.persistence.jooq.Tables.VA_ALLOC)));
        assertEquals(2, db.fetchCount(db.selectFrom(dev.junyoung.vai.persistence.jooq.Tables.VA).where(dev.junyoung.vai.persistence.jooq.Tables.VA.STATUS.eq("ALLOCATED"))));
    }

    @Test
    void shortageRollsBackAllChanges() {
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE')");
        var exception = assertThrows(VaException.class, () -> service.issue(new IssueGeneralVaRequest("T0001", List.of("901", "902"))));
        assertEquals(ErrorCode.AVAILABLE_ACCOUNT_SHORTAGE, exception.errorCode());
        assertEquals("사용 가능한 가상계좌가 부족합니다: 902", exception.getMessage());
        assertEquals(0, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
        assertEquals("AVAILABLE", db.fetchValue("SELECT status FROM va WHERE bank_cd = '901'"));
    }

    @Test
    void concurrentRequestsCannotAllocateTheSameAccount() throws Exception {
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE')");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var jobs = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                start.await();
                try {
                    service.issue(new IssueGeneralVaRequest("T0001", List.of("901")));
                    return true;
                } catch (VaException e) {
                    return false;
                }
            })).toList();
            start.countDown();
            int successes = 0;
            for (var job : jobs) if (job.get()) successes++;
            assertEquals(1, successes);
            assertEquals(1, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_ALLOC));
        }
    }
}
