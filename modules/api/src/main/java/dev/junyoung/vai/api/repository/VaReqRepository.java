package dev.junyoung.vai.api.repository;

import static dev.junyoung.vai.persistence.jooq.Tables.*;

import java.util.HashSet;
import java.util.List;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import dev.junyoung.vai.api.error.ErrorCode;
import dev.junyoung.vai.api.error.VaException;
import dev.junyoung.vai.api.model.ReqStatus;
import dev.junyoung.vai.api.model.VaReq;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VaReqRepository {

	private final DSLContext db;

	public record SaveResult(Long requestId, boolean created) {}

	/// VaReq + VaReqBank save
	public SaveResult save(VaReq vaReq) {
		var inserted = db.insertInto(VA_REQ)
			.set(VA_REQ.TYPE, vaReq.type().name())
			.set(VA_REQ.ORG_CD, vaReq.orgCd())
			.set(VA_REQ.CLIENT_KEY, vaReq.clientKey())
			.set(VA_REQ.QTY, vaReq.qty())
			.set(VA_REQ.STATUS, vaReq.status().name())
			.onConflict(VA_REQ.ORG_CD, VA_REQ.CLIENT_KEY).doNothing()
			.returningResult(VA_REQ.ID).fetchOne();

		if (inserted == null) return new SaveResult(findMatchingRequest(vaReq), false);
		Long vaReqId = inserted.value1();

		var insert = db.insertInto(
			VA_REQ_BANK,
			VA_REQ_BANK.REQ_ID,
			VA_REQ_BANK.BANK_CD,
			VA_REQ_BANK.BANK_ORDER
		);

		for (int i = 0; i < vaReq.bankCodes().size(); i++) {
			insert = insert.values(
				vaReqId,
				vaReq.bankCodes().get(i),
				(short) (i + 1)
			);
		}

		insert.execute();
		return new SaveResult(vaReqId, true);
	}

	private Long findMatchingRequest(VaReq request) {
		var existing = db.selectFrom(VA_REQ)
			.where(VA_REQ.ORG_CD.eq(request.orgCd()).and(VA_REQ.CLIENT_KEY.eq(request.clientKey())))
			.fetchOne();
		if (existing == null) throw new IllegalStateException("요청을 찾을 수 없습니다");
		var banks = db.select(VA_REQ_BANK.BANK_CD).from(VA_REQ_BANK)
			.where(VA_REQ_BANK.REQ_ID.eq(existing.getId()))
			.orderBy(VA_REQ_BANK.BANK_ORDER).fetch(VA_REQ_BANK.BANK_CD);
		if (!existing.getType().equals(request.type().name()) || existing.getQty() != request.qty()
			|| !banks.equals(request.bankCodes())) {
			throw new VaException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, ErrorCode.IDEMPOTENCY_KEY_CONFLICT.display());
		}
		return existing.getId();
	}

	public boolean orgExists(String orgCode) {
		return db.fetchExists(db.selectOne().from(ORG).where(ORG.CODE.eq(orgCode)));
	}

	public boolean banksExist(List<String> bankCodes) {
		return db.fetchCount(db.selectFrom(BANK).where(BANK.CODE.in(bankCodes))) == new HashSet<>(bankCodes).size();
	}

	public void markCompleted(Long reqId) {
		db.update(VA_REQ)
			.set(VA_REQ.STATUS, ReqStatus.COMPLETED.name())
			.where(VA_REQ.ID.eq(reqId))
			.execute();
	}
}
