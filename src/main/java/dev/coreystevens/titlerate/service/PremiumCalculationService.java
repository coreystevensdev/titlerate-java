package dev.coreystevens.titlerate.service;

import dev.coreystevens.titlerate.dto.PremiumRequest;
import dev.coreystevens.titlerate.dto.PremiumResponse;
import dev.coreystevens.titlerate.model.RateSchedule;
import dev.coreystevens.titlerate.repository.RateScheduleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PremiumCalculationService {

    static final String ERR_NO_RATE = "No rate schedule found for state=%s policyType=%s amount=%s";

    private final RateScheduleRepository rates;

    public PremiumCalculationService(RateScheduleRepository rates) {
        this.rates = rates;
    }

    public PremiumResponse calculate(PremiumRequest req) {
        RateSchedule schedule = rates
            .findApplicableSchedule(req.state(), req.policyType(), req.amount())
            .orElseThrow(() -> new IllegalArgumentException(
                ERR_NO_RATE.formatted(req.state(), req.policyType(), req.amount())));

        // rate is expressed as dollars per $1,000 of coverage amount
        BigDecimal thousandths = req.amount().divide(BigDecimal.valueOf(1_000), 10, RoundingMode.HALF_UP);
        BigDecimal basePremium = thousandths
            .multiply(schedule.getRatePerThousand())
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO;
        if (req.simultaneousIssue() && schedule.getSimultaneousDiscountPct().compareTo(BigDecimal.ZERO) > 0) {
            discount = basePremium
                .multiply(schedule.getSimultaneousDiscountPct())
                .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal net = basePremium.subtract(discount);

        return new PremiumResponse(
            req.state(),
            req.policyType(),
            req.amount(),
            basePremium,
            discount,
            net,
            schedule.getRatePerThousand()
        );
    }
}
