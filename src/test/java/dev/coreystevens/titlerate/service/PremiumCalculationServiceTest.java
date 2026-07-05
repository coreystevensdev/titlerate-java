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
import java.util.Optional;

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

    private RateSchedule paOwnerSchedule(BigDecimal rate, BigDecimal discountPct) {
        return new RateSchedule("PA", PolicyType.OWNER,
            BigDecimal.ZERO, new BigDecimal("100000"),
            rate, discountPct, LocalDate.of(2024, 1, 1));
    }

    @Test
    void calculatesBasePremiumCorrectly() {
        // $500,000 at $3.00 per thousand = $1,500.00
        when(rateRepo.findApplicableSchedule(eq("PA"), eq(PolicyType.OWNER), any()))
            .thenReturn(Optional.of(paOwnerSchedule(new BigDecimal("3.00"), BigDecimal.ZERO)));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("500000"), false));

        assertThat(resp.basePremium()).isEqualByComparingTo("1500.00");
        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("0.00");
        assertThat(resp.netPremium()).isEqualByComparingTo("1500.00");
    }

    @Test
    void appliesSimultaneousDiscountWhenRequested() {
        // $200,000 at $2.75/k with 30% simultaneous discount
        when(rateRepo.findApplicableSchedule(eq("PA"), eq(PolicyType.LENDER), any()))
            .thenReturn(Optional.of(paOwnerSchedule(new BigDecimal("2.75"), new BigDecimal("0.30"))));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("PA", PolicyType.LENDER, new BigDecimal("200000"), true));

        // base = 200 * 2.75 = 550.00, discount = 550 * 0.30 = 165.00, net = 385.00
        assertThat(resp.basePremium()).isEqualByComparingTo("550.00");
        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("165.00");
        assertThat(resp.netPremium()).isEqualByComparingTo("385.00");
    }

    @Test
    void skipsDiscountWhenSimultaneousIssueIsFalse() {
        when(rateRepo.findApplicableSchedule(eq("NJ"), eq(PolicyType.LENDER), any()))
            .thenReturn(Optional.of(paOwnerSchedule(new BigDecimal("3.00"), new BigDecimal("0.25"))));

        PremiumResponse resp = service.calculate(
            new PremiumRequest("NJ", PolicyType.LENDER, new BigDecimal("100000"), false));

        assertThat(resp.simultaneousDiscount()).isEqualByComparingTo("0.00");
        assertThat(resp.netPremium()).isEqualByComparingTo(resp.basePremium());
    }

    @Test
    void throwsWhenNoRateScheduleFound() {
        when(rateRepo.findApplicableSchedule(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(
            new PremiumRequest("PA", PolicyType.OWNER, new BigDecimal("500000"), false)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No rate schedule found");
    }
}
