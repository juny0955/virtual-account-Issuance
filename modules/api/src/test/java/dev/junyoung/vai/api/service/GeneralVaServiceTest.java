package dev.junyoung.vai.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import dev.junyoung.vai.api.controller.GeneralVaController;
import dev.junyoung.vai.api.controller.BulkVaController;
import dev.junyoung.vai.api.repository.VaReqRepository;
import dev.junyoung.vai.api.repository.VaGroupRepository;
import dev.junyoung.vai.api.repository.VaRepository;
import dev.junyoung.vai.api.repository.VaAllocRepository;
import dev.junyoung.vai.api.controller.dto.IssueGeneralVaRequest;
import dev.junyoung.vai.api.controller.dto.IssueBulkVaRequest;
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
    @Autowired BulkVaService bulkService;
    @Autowired DSLContext db;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired VaGroupRepository groups;
    @Autowired VaRepository accounts;
    @Autowired VaAllocRepository allocations;

    @BeforeEach
    void setup() {
        db.execute("TRUNCATE va_alloc, va_bulk_result, va_group, va_req_bank, va_req, va, org CASCADE");
        db.execute("INSERT INTO org(code, name) VALUES ('T0001', 'Test')");
        db.execute("INSERT INTO bank(code, name) VALUES ('901', 'Test1'), ('902', 'Test2') ON CONFLICT (code) DO NOTHING");
    }

    @Test
    void issuesAccountsInRequestedOrder() {
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE'), ('902', 'b', 'AVAILABLE')");
        var result = service.issue(new IssueGeneralVaRequest("T0001", List.of("902", "901")), "test-key");
        assertEquals(List.of(new IssueResult.Account("902", "b"), new IssueResult.Account("901", "a")), result.accounts());
        assertEquals("COMPLETED", db.fetchValue("SELECT status FROM va_req WHERE id = ?", result.requestId()));
        assertEquals(List.of("902", "901"), db.fetch("SELECT bank_cd FROM va_req_bank WHERE req_id = ? ORDER BY bank_order", result.requestId()).getValues(0, String.class));
        assertEquals(2, db.fetchCount(db.selectFrom(dev.junyoung.vai.persistence.jooq.Tables.VA_ALLOC)));
        assertEquals(2, db.fetchCount(db.selectFrom(dev.junyoung.vai.persistence.jooq.Tables.VA).where(dev.junyoung.vai.persistence.jooq.Tables.VA.STATUS.eq("ALLOCATED"))));
    }

    @Test
    void shortageRollsBackAllChanges() {
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE')");
        var exception = assertThrows(VaException.class, () -> service.issue(new IssueGeneralVaRequest("T0001", List.of("901", "902")), "test-key"));
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
                    service.issue(new IssueGeneralVaRequest("T0001", List.of("901")), "test-key-" + i);
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

    @Test
    void retryReturnsOriginalAccountsAndRejectsChangedRequest() {
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE'), ('902', 'b', 'AVAILABLE')");
        var request = new IssueGeneralVaRequest("T0001", List.of("902", "901"));
        var first = service.issue(request, "retry-key");
        assertEquals(first, service.issue(request, "retry-key"));
        assertEquals("retry-key", db.fetchValue("SELECT client_key FROM va_req WHERE id = ?", first.requestId()));
        var conflict = assertThrows(VaException.class, () -> service.issue(
            new IssueGeneralVaRequest("T0001", List.of("901", "902")), "retry-key"));
        assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, conflict.errorCode());
        assertEquals(1, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
        assertEquals(2, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_ALLOC));
    }

    @Test
    void concurrentGeneralRequestsFromSeparateInstancesReturnSameResponse() throws Exception {
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE')");
        var request = new IssueGeneralVaRequest("T0001", List.of("901"));
        verifyConcurrentRequests(repo -> new GeneralVaController(
            new GeneralVaService(repo, groups, accounts, allocations)).issue("same-key", request));
        assertEquals(1, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
        assertEquals(1, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_ALLOC));
    }

    @Test
    void concurrentBulkRequestsFromSeparateInstancesReturnSameResponse() throws Exception {
        var request = new IssueBulkVaRequest("T0001", List.of("901"), 2);
        verifyConcurrentRequests(repo -> new BulkVaController(new BulkVaService(repo)).issue("same-key", request));
        assertEquals(1, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
    }

    private <T> void verifyConcurrentRequests(Function<VaReqRepository, T> issue) throws Exception {
        var firstSaved = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var firstRepo = spy(new VaReqRepository(db));
        var secondRepo = spy(new VaReqRepository(db));
        doAnswer(call -> {
            var result = call.callRealMethod();
            firstSaved.countDown();
            if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new AssertionError("first transaction timed out");
            return result;
        }).when(firstRepo).save(any());
        doAnswer(call -> {
            secondStarted.countDown();
            return call.callRealMethod();
        }).when(secondRepo).save(any());
        var transactions = new TransactionTemplate(transactionManager);
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var first = executor.submit(() -> transactions.execute(status -> issue.apply(firstRepo)));
                assertTrue(firstSaved.await(5, TimeUnit.SECONDS));
                var second = executor.submit(() -> transactions.execute(status -> issue.apply(secondRepo)));
                assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean blocked = false;
                while (System.nanoTime() < deadline) {
                    blocked = db.fetchOne("SELECT EXISTS (SELECT 1 FROM pg_stat_activity "
                        + "WHERE pid <> pg_backend_pid() AND cardinality(pg_blocking_pids(pid)) > 0 "
                        + "AND query ILIKE '%va_req%')").get(0, Boolean.class);
                    if (blocked) break;
                    Thread.sleep(20);
                }
                assertTrue(blocked, "second insert did not wait for the first transaction");
                releaseFirst.countDown();
                assertEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            } finally {
                releaseFirst.countDown();
            }
        }
    }

    @Test
    void keysAreScopedToOrganizationAndSharedAcrossRequestTypes() {
        db.execute("INSERT INTO org(code, name) VALUES ('T0002', 'Test2')");
        db.execute("INSERT INTO va(bank_cd, va_no, status) VALUES ('901', 'a', 'AVAILABLE'), ('901', 'b', 'AVAILABLE')");
        service.issue(new IssueGeneralVaRequest("T0001", List.of("901")), "shared-key");
        service.issue(new IssueGeneralVaRequest("T0002", List.of("901")), "shared-key");
        var conflict = assertThrows(VaException.class, () -> bulkService.issue(
            new IssueBulkVaRequest("T0001", List.of("901"), 1), "shared-key"));
        assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, conflict.errorCode());
        assertEquals(2, db.fetchCount(dev.junyoung.vai.persistence.jooq.Tables.VA_REQ));
    }
}
