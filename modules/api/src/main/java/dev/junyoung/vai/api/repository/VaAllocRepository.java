package dev.junyoung.vai.api.repository;

import static dev.junyoung.vai.persistence.jooq.Tables.VA_ALLOC;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import dev.junyoung.vai.api.model.VaAlloc;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VaAllocRepository {
	private final DSLContext db;

	public void saveAll(List<VaAlloc> allocs) {
		if (allocs.isEmpty()) {
			return;
		}
		db.insertInto(VA_ALLOC, VA_ALLOC.REQ_ID, VA_ALLOC.GROUP_ID, VA_ALLOC.BANK_CD, VA_ALLOC.VA_NO)
			.valuesOfRows(allocs.stream()
				.map(alloc -> DSL.row(alloc.reqId(), alloc.groupId(), alloc.bankCd(), alloc.vaNo()))
				.toList())
			.execute();
	}
}
