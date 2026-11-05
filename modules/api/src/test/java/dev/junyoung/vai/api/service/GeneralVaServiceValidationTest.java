package dev.junyoung.vai.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.junyoung.vai.api.controller.dto.IssueGeneralVaRequest;
import dev.junyoung.vai.api.error.ErrorCode;
import dev.junyoung.vai.api.error.VaException;
import dev.junyoung.vai.api.repository.VaAllocRepository;
import dev.junyoung.vai.api.repository.VaGroupRepository;
import dev.junyoung.vai.api.repository.VaRepository;
import dev.junyoung.vai.api.repository.VaReqRepository;

class GeneralVaServiceValidationTest {
    @Test
    void rejectsUnknownOrganizationOrBankBeforeSaving() {
        var requests = mock(VaReqRepository.class);
        var groups = mock(VaGroupRepository.class);
        var accounts = mock(VaRepository.class);
        var allocations = mock(VaAllocRepository.class);
        var service = new GeneralVaService(requests, groups, accounts, allocations);
        when(requests.orgExists("T0001")).thenReturn(true);

        for (var request : List.of(
                new IssueGeneralVaRequest("T9999", List.of("901")),
                new IssueGeneralVaRequest("T0001", List.of("999")))) {
            var exception = assertThrows(VaException.class, () -> service.issue(request));
            assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode());
        }
        verify(requests, never()).save(any());
        verifyNoInteractions(groups, accounts, allocations);
    }
}
