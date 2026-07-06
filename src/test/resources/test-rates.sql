DELETE FROM rate_schedules;

INSERT INTO rate_schedules (state, policy_type, tier_start, rate_per_thousand, simultaneous_discount_pct, effective_date)
VALUES ('PA', 'OWNER', 0.00, 3.5000, 0.0000, '2024-01-01');

INSERT INTO rate_schedules (state, policy_type, tier_start, rate_per_thousand, simultaneous_discount_pct, effective_date)
VALUES ('PA', 'LENDER', 0.00, 2.7500, 0.3000, '2024-01-01');
