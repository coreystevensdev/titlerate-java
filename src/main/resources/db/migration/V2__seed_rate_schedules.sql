-- Illustrative tiers. Verify against current state filings before treating any
-- quote from this service as authoritative.
--
-- Was data.sql, which Spring re-ran on every boot alongside ddl-auto=create-drop.
-- As a migration it runs once and survives a restart.
INSERT INTO rate_schedules (state, policy_type, tier_start, tier_end, rate_per_thousand, simultaneous_discount_pct, effective_date)
VALUES
  ('PA', 'OWNER',  0,        100000,   3.50, 0.00, '2024-01-01'),
  ('PA', 'OWNER',  100000,   1000000,  3.00, 0.00, '2024-01-01'),
  ('PA', 'OWNER',  1000000,  NULL,     2.50, 0.00, '2024-01-01'),
  ('PA', 'LENDER', 0,        100000,   2.75, 0.30, '2024-01-01'),
  ('PA', 'LENDER', 100000,   1000000,  2.25, 0.30, '2024-01-01'),
  ('PA', 'LENDER', 1000000,  NULL,     2.00, 0.30, '2024-01-01'),
  ('NJ', 'OWNER',  0,        200000,   3.75, 0.00, '2024-01-01'),
  ('NJ', 'OWNER',  200000,   1000000,  3.00, 0.00, '2024-01-01'),
  ('NJ', 'OWNER',  1000000,  NULL,     2.50, 0.00, '2024-01-01'),
  ('NJ', 'LENDER', 0,        200000,   3.00, 0.25, '2024-01-01'),
  ('NJ', 'LENDER', 200000,   1000000,  2.50, 0.25, '2024-01-01'),
  ('NJ', 'LENDER', 1000000,  NULL,     2.00, 0.25, '2024-01-01');
