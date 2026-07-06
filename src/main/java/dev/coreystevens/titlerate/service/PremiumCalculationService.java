package dev.coreystevens.titlerate.service;

import dev.coreystevens.titlerate.dto.PremiumRequest;
import dev.coreystevens.titlerate.dto.PremiumResponse;
import dev.coreystevens.titlerate.dto.TierBreakdown;
import dev.coreystevens.titlerate.model.RateSchedule;
import dev.coreystevens.titlerate.repository.RateScheduleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PremiumCalculationService {

    static final String ERR_NO_RATE = "No rate schedule found for state=%s policyType=%s amount=%s";

    private final RateScheduleRepository rates;

    public PremiumCalculationService(RateScheduleRepository rates) {
        this.rates = rates;
    }

    public PremiumResponse calculate(PremiumRequest req) {
        List<RateSchedule> tiers = rates.findByStateAndPolicyTypeOrderByTierStart(
            req.state(), req.policyType());

        if (tiers.isEmpty()) {
            throw new IllegalArgumentException(
                ERR_NO_RATE.formatted(req.state(), req.policyType(), req.amount()));
        }

        BigDecimal remaining = req.amount();
        BigDecimal basePremium = BigDecimal.ZERO;
        List<TierBreakdown> breakdown = new ArrayList<>();

        for (RateSchedule tier : tiers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            // null tierEnd means this tier absorbs all remaining amount (open-ended)
            BigDecimal tierCapacity = (tier.getTierEnd() != null)
                ? tier.getTierEnd().subtract(tier.getTierStart())
                : remaining;

            BigDecimal amountInTier = remaining.min(tierCapacity);
            BigDecimal tierPremium = amountInTier
                .divide(BigDecimal.valueOf(1_000), 10, RoundingMode.HALF_UP)
                .multiply(tier.getRatePerThousand())
                .setScale(2, RoundingMode.HALF_UP);

            breakdown.add(new TierBreakdown(
                tier.getTierStart(), tier.getTierEnd(),
                amountInTier, tier.getRatePerThousand(), tierPremium));

            basePremium = basePremium.add(tierPremium);
            remaining = remaining.subtract(amountInTier);
        }

        // Simultaneous discount pct is uniform across all tiers for a given state + policy type.
        BigDecimal discountPct = tiers.get(0).getSimultaneousDiscountPct();
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (req.simultaneousIssue() && discountPct.compareTo(BigDecimal.ZERO) > 0) {
            discount = basePremium
                .multiply(discountPct)
                .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal net = basePremium.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal lastRate = tiers.get(tiers.size() - 1).getRatePerThousand();

        return new PremiumResponse(
            req.state(), req.policyType(), req.amount(),
            basePremium, discount, net, lastRate, breakdown);
    }
}
