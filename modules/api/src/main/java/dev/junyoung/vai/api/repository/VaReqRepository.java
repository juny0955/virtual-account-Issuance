package dev.junyoung.vai.api.repository;

import static dev.junyoung.vai.persistence.jooq.Tables.*;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import dev.junyoung.vai.api.model.ReqStatus;
import dev.junyoung.vai.api.model.VaReq;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VaReqRepository {

	private final DSLContext db;

	/// VaReq + VaReqBank save
	public Long save(VaReq vaReq) {
		Long vaReqId = db.insertInto(VA_REQ)
			.set(VA_REQ.TYPE, vaReq.type().name())
			.set(VA_REQ.ORG_CD, vaReq.orgCd())
			.set(VA_REQ.QTY, vaReq.qty())
			.set(VA_REQ.STATUS, vaReq.status().name())
			.returningResult(VA_REQ.ID).fetchOne(VA_REQ.ID);

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
		return vaReqId;
	}
	public void markCompleted(Long reqId) {
		db.update(VA_REQ)
			.set(VA_REQ.STATUS, ReqStatus.COMPLETED.name())
			.where(VA_REQ.ID.eq(reqId))
			.execute();
	}
}
