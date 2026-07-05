package dev.coreystevens.titlerate.dto;

import dev.coreystevens.titlerate.model.PolicyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record PremiumRequest(
    @NotBlank @Pattern(regexp = "PA|NJ", message = "state must be PA or NJ")
    String state,

    @NotNull
    PolicyType policyType,

    @NotNull @DecimalMin(value = "1.00", message = "amount must be at least $1")
    BigDecimal amount,

    boolean simultaneousIssue
) {}
