package dev.junyoung.vai.api.model;

import java.time.OffsetDateTime;

public record Va(
	String bankCd,
	String vaNo,
	VaStatus status,
	OffsetDateTime reusable_at,
	OffsetDateTime created_at
) {
}
