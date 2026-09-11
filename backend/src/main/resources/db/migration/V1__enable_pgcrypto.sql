-- gen_random_uuid() requires pgcrypto on older Postgres (built-in from PG13+
-- via pgcrypto, or natively from PG13+ with gen_random_uuid in core from PG13).
-- Safe to run even where it's already available.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
