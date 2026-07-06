package dev.coreystevens.titlerate.dto;

import dev.coreystevens.titlerate.model.PolicyType;

import java.math.BigDecimal;
import java.util.List;

public record PremiumResponse(
    String state,
    PolicyType policyType,
    BigDecimal amount,
    BigDecimal basePremium,
    BigDecimal simultaneousDiscount,
    BigDecimal netPremium,
    BigDecimal ratePerThousandApplied,
    List<TierBreakdown> breakdown
) {}
