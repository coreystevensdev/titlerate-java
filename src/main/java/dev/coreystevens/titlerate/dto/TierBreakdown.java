package dev.coreystevens.titlerate.dto;

import java.math.BigDecimal;

public record TierBreakdown(
    BigDecimal tierStart,
    BigDecimal tierEnd,
    BigDecimal amountInTier,
    BigDecimal ratePerThousand,
    BigDecimal premium
) {}
