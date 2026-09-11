-- Distributed Job Scheduler: initial schema

CREATE TABLE app_users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER'
);

CREATE TABLE jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    cron_expression VARCHAR(100) NOT NULL,
    payload         TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    max_retries     INT          NOT NULL DEFAULT 3,
    next_run_at     TIMESTAMPTZ,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_jobs_name ON jobs (LOWER(name));

-- The single most important index in the system: the scheduler's core
-- query is "find ACTIVE jobs whose next_run_at is due", every tick.
CREATE INDEX idx_jobs_status_next_run ON jobs (status, next_run_at);

CREATE TABLE job_dependencies (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id            UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    depends_on_job_id UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT uq_job_dependency UNIQUE (job_id, depends_on_job_id),
    CONSTRAINT chk_no_self_dependency CHECK (job_id <> depends_on_job_id)
);

CREATE INDEX idx_job_dependencies_job_id ON job_dependencies (job_id);
CREATE INDEX idx_job_dependencies_depends_on ON job_dependencies (depends_on_job_id);

CREATE TABLE executions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id       UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    status       VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    worker_id    VARCHAR(100),
    retry_count  INT NOT NULL DEFAULT 0,
    queued_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at   TIMESTAMPTZ,
    finished_at  TIMESTAMPTZ,
    log          TEXT
);

CREATE INDEX idx_executions_job_id ON executions (job_id, queued_at DESC);
CREATE INDEX idx_executions_queued_at ON executions (queued_at DESC);
