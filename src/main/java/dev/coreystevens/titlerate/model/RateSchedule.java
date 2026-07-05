package dev.coreystevens.titlerate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "rate_schedules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"state", "policy_type", "tier_start"})
)
public class RateSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 2, nullable = false)
    private String state;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private PolicyType policyType;

    @NotNull
    @Column(name = "tier_start", nullable = false, precision = 14, scale = 2)
    private BigDecimal tierStart;

    @Column(name = "tier_end", precision = 14, scale = 2)
    private BigDecimal tierEnd;

    @NotNull
    @Column(name = "rate_per_thousand", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerThousand;

    @NotNull
    @Column(name = "simultaneous_discount_pct", nullable = false, precision = 5, scale = 4)
    private BigDecimal simultaneousDiscountPct;

    @NotNull
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    protected RateSchedule() {}

    public RateSchedule(String state, PolicyType policyType, BigDecimal tierStart,
                        BigDecimal tierEnd, BigDecimal ratePerThousand,
                        BigDecimal simultaneousDiscountPct, LocalDate effectiveDate) {
        this.state = state;
        this.policyType = policyType;
        this.tierStart = tierStart;
        this.tierEnd = tierEnd;
        this.ratePerThousand = ratePerThousand;
        this.simultaneousDiscountPct = simultaneousDiscountPct;
        this.effectiveDate = effectiveDate;
    }

    public Long getId() { return id; }
    public String getState() { return state; }
    public PolicyType getPolicyType() { return policyType; }
    public BigDecimal getTierStart() { return tierStart; }
    public BigDecimal getTierEnd() { return tierEnd; }
    public BigDecimal getRatePerThousand() { return ratePerThousand; }
    public BigDecimal getSimultaneousDiscountPct() { return simultaneousDiscountPct; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
}
