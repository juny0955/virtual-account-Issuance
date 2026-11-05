package dev.junyoung.vai.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.junyoung.vai.api.controller.dto.IssueBulkVaRequest;
import dev.junyoung.vai.api.error.ErrorCode;
import dev.junyoung.vai.api.error.VaException;
import dev.junyoung.vai.api.model.VaReq;
import dev.junyoung.vai.api.repository.VaReqRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BulkVaService {
    private final VaReqRepository vaReqRepository;

    @Transactional
    public void issue(IssueBulkVaRequest request, String clientKey) {
        if (!vaReqRepository.orgExists(request.orgCd()) || !vaReqRepository.banksExist(request.bankCodes())) {
            throw new VaException(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.display());
        }
        var vaReq = VaReq.newBulk(request.orgCd(), clientKey, request.qty(), request.bankCodes());
        vaReqRepository.save(vaReq);
    }
}
