package dev.junyoung.vai.api.repository;

import static dev.junyoung.vai.persistence.jooq.Tables.*;

import java.util.Optional;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import dev.junyoung.vai.api.model.Va;
import dev.junyoung.vai.api.model.VaStatus;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VaRepository {
	private final DSLContext db;

	public Optional<Va> allocateAvailable(String bankCode) {
		var record = db.update(VA)
			.set(VA.STATUS, VaStatus.ALLOCATED.name())
			.where(VA.BANK_CD.eq(bankCode), VA.VA_NO.eq(
				db.select(VA.VA_NO)
					.from(VA)
					.where(VA.BANK_CD.eq(bankCode), VA.STATUS.eq(VaStatus.AVAILABLE.name()))
					.orderBy(VA.VA_NO)
					.limit(1)
					.forUpdate()
					.skipLocked()))
			.returning()
			.fetchOne();

		if (record == null) {
			return Optional.empty();
		}

		return Optional.of(new Va(
			record.getBankCd(),
			record.getVaNo(),
			VaStatus.valueOf(record.getStatus()),
			record.getReusableAt(),
			record.getCreatedAt()
		));
	}
}
