package dev.junyoung.vai.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.junyoung.vai.api.controller.dto.IssueGeneralVaRequest;
import dev.junyoung.vai.api.error.ErrorCode;
import dev.junyoung.vai.api.error.VaException;
import dev.junyoung.vai.api.model.Va;
import dev.junyoung.vai.api.model.VaAlloc;
import dev.junyoung.vai.api.model.VaGroup;
import dev.junyoung.vai.api.model.VaReq;
import dev.junyoung.vai.api.repository.VaAllocRepository;
import dev.junyoung.vai.api.repository.VaGroupRepository;
import dev.junyoung.vai.api.repository.VaRepository;
import dev.junyoung.vai.api.repository.VaReqRepository;
import dev.junyoung.vai.api.service.result.IssueResult;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneralVaService {
	private final VaReqRepository vaReqRepository;
	private final VaGroupRepository vaGroupRepository;
	private final VaRepository vaRepository;
	private final VaAllocRepository vaAllocRepository;

	@Transactional
	public IssueResult issue(IssueGeneralVaRequest request, String clientKey) {
		if (!vaReqRepository.orgExists(request.orgCd()) || !vaReqRepository.banksExist(request.bankCodes())) {
			throw new VaException(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.display());
		}
		VaReq vaReq = VaReq.newGeneral(request.orgCd(), clientKey, request.bankCodes());
		var saved = vaReqRepository.save(vaReq);
		Long vaReqId = saved.requestId();
		if (!saved.created()) {
			return IssueResult.of(vaReqId, request.bankCodes(), vaAllocRepository.findAccountsByRequestId(vaReqId));
		}

		Long groupId = vaGroupRepository.save(VaGroup.newGeneral(vaReqId));

		Map<String, String> accounts = new HashMap<>();
		List<VaAlloc> allocations = new ArrayList<>();
		for (String bankCode : request.bankCodes()) {
			Va va = vaRepository.allocateAvailable(bankCode)
				.orElseThrow(() -> new VaException(ErrorCode.AVAILABLE_ACCOUNT_SHORTAGE,
					ErrorCode.AVAILABLE_ACCOUNT_SHORTAGE.display() + ": " + bankCode));
			allocations.add(new VaAlloc(vaReqId, groupId, bankCode, va.vaNo()));
			accounts.put(bankCode, va.vaNo());
		}

		vaAllocRepository.saveAll(allocations);
		vaReqRepository.markCompleted(vaReqId);
		return IssueResult.of(vaReqId, request.bankCodes(), accounts);
	}
}
