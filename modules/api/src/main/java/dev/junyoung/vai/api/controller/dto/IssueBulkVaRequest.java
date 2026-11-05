package dev.junyoung.vai.api.controller.dto;

import java.util.List;
import java.util.HashSet;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

public record IssueBulkVaRequest(
	@NotBlank @Size(max = 5) String orgCd,
	@NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 3) String> bankCodes,
	@NotNull @Positive Integer qty
) {
	@AssertTrue
	public boolean isBankCodesUnique() {
		return bankCodes == null || new HashSet<>(bankCodes).size() == bankCodes.size();
	}
}
