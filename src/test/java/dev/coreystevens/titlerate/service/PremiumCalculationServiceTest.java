package dev.coreystevens.titlerate.service;

import dev.coreystevens.titlerate.dto.PremiumRequest;
import dev.coreystevens.titlerate.dto.PremiumResponse;
import dev.coreystevens.titlerate.model.PolicyType;
import dev.coreystevens.titlerate.model.RateSchedule;
import dev.coreystevens.titlerate.repository.RateScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PremiumCalculationServiceTest {

    @Mock
    private RateScheduleRepository rateRepo;

    private PremiumCalculationService service;

    @BeforeEach
    void setUp() {
        service = new PremiumCalculationService(rateRepo);
    }

    private RateSchedule tier(String state, PolicyType type,
                              BigDecimal start, BigDecimal end,
                              BigDecimal rate, BigDecimal discountPct) {
        return new RateSchedule(state, type, start, end, rate, discountPct, LocalDate.of(2024, 1, 1));
    }

    // Single open-ended tier: entire amount at one rate
    @Test
    void singleTierOpenEnded_flatRate() {
        // $500,000 at $3.00/thousand with no upper bound = $1,500.00
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.OWNER,
                    BigDecimal.ZERO, null,
                    new BigDecimal("3.00"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("500000"), false));

        assertThat(resp.basePremium()).isEqualByComparingTo("1500.00");
        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("0.00");
        assertThat(resp.netPremium()).isEqualByComparingTo("1500.00");
        assertThat(resp.breakdown()).hasSize(1);
    }

    // Two-tier bracket accumulation: amount spans both tiers
    @Test
    void multiTier_accumulatesAcrossBrackets() {
        // Tier 1: $0-$100k @ $4.00/thousand
        // Tier 2: $100k+ (open-ended) @ $2.00/thousand
        // Amount: $200,000
        // Expected: (100 * 4.00) + (100 * 2.00) = 400 + 200 = $600.00
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.OWNER,
                    BigDecimal.ZERO, new BigDecimal("100000"),
                    new BigDecimal("4.00"), BigDecimal.ZERO),
                tier("PA", PolicyType.OWNER,
                    new BigDecimal("100000"), null,
                    new BigDecimal("2.00"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("200000"), false));

        assertThat(resp.basePremium()).isEqualByComparingTo("600.00");
        assertThat(resp.breakdown()).hasSize(2);
        assertThat(resp.breakdown().get(0).premium()).isEqualByComparingTo("400.00");
        assertThat(resp.breakdown().get(1).premium()).isEqualByComparingTo("200.00");
    }

    // Simultaneous discount applied to the bracket-accumulated base
    @Test
    void multiTier_appliesSimultaneousDiscountOnTotalBase() {
        // Same two tiers, base = $600.00, 30% discount = $180.00, net = $420.00
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.LENDER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.LENDER,
                    BigDecimal.ZERO, new BigDecimal("100000"),
                    new BigDecimal("4.00"), new BigDecimal("0.30")),
                tier("PA", PolicyType.LENDER,
                    new BigDecimal("100000"), null,
                    new BigDecimal("2.00"), new BigDecimal("0.30"))));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.LENDER, new BigDecimal("200000"), true));

        assertThat(resp.basePremium()).isEqualByComparingTo("600.00");
        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("180.00");
        assertThat(resp.netPremium()).isEqualByComparingTo("420.00");
    }

    // Discount is skipped when simultaneousIssue is false
    @Test
    void skipsDiscountWhenSimultaneousIssueIsFalse() {
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("NJ"), eq(PolicyType.LENDER)))
            .thenReturn(List.of(
                tier("NJ", PolicyType.LENDER,
                    BigDecimal.ZERO, null,
                    new BigDecimal("3.00"), new BigDecimal("0.25"))));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("NJ", PolicyType.LENDER, new BigDecimal("100000"), false));

        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("0.00");
        assertThat(resp.netPremium()).isEqualByComparingTo(resp.basePremium());
    }

    // Amount fits entirely in the first tier (does not spill into tier 2)
    @Test
    void multiTier_amountFitsInFirstTier() {
        // Amount $50,000 fits entirely within the $0-$100k tier at $4.00/k = $200.00
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.OWNER,
                    BigDecimal.ZERO, new BigDecimal("100000"),
                    new BigDecimal("4.00"), BigDecimal.ZERO),
                tier("PA", PolicyType.OWNER,
                    new BigDecimal("100000"), null,
                    new BigDecimal("2.00"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("50000"), false));

        assertThat(resp.basePremium()).isEqualByComparingTo("200.00");
        assertThat(resp.breakdown().get(0).amountInTier()).isEqualByComparingTo("50000");
    }

    @Test
    void throwsWhenNoRateScheduleFound() {
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(any(), any()))
            .thenReturn(List.of());

        assertThatThrownBy(() -> service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("500000"), false)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No rate schedule found");
    }
}
