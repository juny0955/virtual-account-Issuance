package dev.junyoung.vai.api.repository;

import static dev.junyoung.vai.persistence.jooq.Tables.VA_GROUP;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import dev.junyoung.vai.api.model.VaGroup;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VaGroupRepository {
	private final DSLContext db;

	public Long save(VaGroup group) {
		return db.insertInto(VA_GROUP)
			.set(VA_GROUP.REQ_ID, group.reqId())
			.set(VA_GROUP.ISSUE_NO, group.issueNo())
			.returningResult(VA_GROUP.ID).fetchOne(VA_GROUP.ID);
	}
}
