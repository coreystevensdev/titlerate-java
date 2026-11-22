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
        assertThat(resp.breakdown()).hasSize(1);
        assertThat(resp.ratePerThousandApplied()).isEqualByComparingTo("4.00");
    }

    // The reported rate has to come from the tier the amount reached, not the top bracket on file.
    @Test
    void ratePerThousandApplied_isTheTierTheAmountReached() {
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.OWNER,
                    BigDecimal.ZERO, new BigDecimal("100000"),
                    new BigDecimal("3.50"), BigDecimal.ZERO),
                tier("PA", PolicyType.OWNER,
                    new BigDecimal("100000"), new BigDecimal("1000000"),
                    new BigDecimal("3.00"), BigDecimal.ZERO),
                tier("PA", PolicyType.OWNER,
                    new BigDecimal("1000000"), null,
                    new BigDecimal("2.50"), BigDecimal.ZERO)));

        PremiumResponse midTier = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("350000"), false));
        assertThat(midTier.ratePerThousandApplied()).isEqualByComparingTo("3.00");

        PremiumResponse topTier = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("2000000"), false));
        assertThat(topTier.ratePerThousandApplied()).isEqualByComparingTo("2.50");
    }

    // An amount landing exactly on a tier boundary stops there. Spilling a zero-amount
    // row into the next tier would also move the reported rate to the next bracket.
    @Test
    void amountExactlyOnTierBoundary_staysInThatTier() {
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.OWNER,
                    BigDecimal.ZERO, new BigDecimal("100000"),
                    new BigDecimal("4.00"), BigDecimal.ZERO),
                tier("PA", PolicyType.OWNER,
                    new BigDecimal("100000"), null,
                    new BigDecimal("2.00"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("100000"), false));

        assertThat(resp.breakdown()).hasSize(1);
        assertThat(resp.basePremium()).isEqualByComparingTo("400.00");
        assertThat(resp.ratePerThousandApplied()).isEqualByComparingTo("4.00");
    }

    // Each tier rounds to cents before the tiers are summed. Rounding once at the end
    // instead gives a different total, so the choice is worth pinning.
    @Test
    void roundsEachTierBeforeSumming() {
        // tier 1: 1,250 / 1000 * 3.33 = 4.1625 -> 4.16
        // tier 2:   500 / 1000 * 3.33 = 1.665  -> 1.67
        // per-tier total 5.83; rounding the unrounded sum (5.8275) would give 5.83 too,
        // so the assertion that separates them is the per-tier figure.
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.OWNER,
                    BigDecimal.ZERO, new BigDecimal("1250"),
                    new BigDecimal("3.33"), BigDecimal.ZERO),
                tier("PA", PolicyType.OWNER,
                    new BigDecimal("1250"), null,
                    new BigDecimal("3.33"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("1750"), false));

        assertThat(resp.breakdown().get(0).premium()).isEqualByComparingTo("4.16");
        assertThat(resp.breakdown().get(1).premium()).isEqualByComparingTo("1.67");
        assertThat(resp.basePremium()).isEqualByComparingTo("5.83");
    }

    // simultaneousIssue is true but the schedule carries no discount, so the guard on
    // discountPct is what keeps this at zero rather than the flag.
    @Test
    void simultaneousIssueWithZeroDiscountPct_chargesFullBase() {
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("NJ"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("NJ", PolicyType.OWNER,
                    BigDecimal.ZERO, null,
                    new BigDecimal("3.00"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("NJ", PolicyType.OWNER, new BigDecimal("250000"), true));

        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("0.00");
        assertThat(resp.netPremium()).isEqualByComparingTo("750.00");
    }

    // The discount rounds half up on its own, so a half-cent discount rounds against
    // the buyer rather than being truncated.
    @Test
    void discountRoundsHalfUp() {
        // base 333.00 * 0.125 = 41.625 -> 41.63, net 291.37
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.LENDER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.LENDER,
                    BigDecimal.ZERO, null,
                    new BigDecimal("3.33"), new BigDecimal("0.125"))));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.LENDER, new BigDecimal("100000"), true));

        assertThat(resp.basePremium()).isEqualByComparingTo("333.00");
        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("41.63");
        assertThat(resp.netPremium()).isEqualByComparingTo("291.37");
    }

    // A policy small enough that its premium rounds below a cent quotes zero rather
    // than failing. Worth knowing before someone treats a 0.00 quote as an error.
    @Test
    void amountTooSmallToRoundToACent_quotesZero() {
        when(rateRepo.findByStateAndPolicyTypeOrderByTierStart(eq("PA"), eq(PolicyType.OWNER)))
            .thenReturn(List.of(
                tier("PA", PolicyType.OWNER,
                    BigDecimal.ZERO, null,
                    new BigDecimal("4.00"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("1"), false));

        assertThat(resp.basePremium()).isEqualByComparingTo("0.00");
        assertThat(resp.breakdown()).hasSize(1);
        assertThat(resp.breakdown().get(0).amountInTier()).isEqualByComparingTo("1");
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
