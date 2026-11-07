CREATE TABLE session (
    id             UUID PRIMARY KEY,
    problem_id     VARCHAR(100) NOT NULL,
    mode           VARCHAR(20)  NOT NULL,
    participant_id VARCHAR(50),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE submission (
    id                  UUID PRIMARY KEY,
    session_id          UUID         NOT NULL REFERENCES session (id),
    version             INT          NOT NULL,
    code                TEXT         NOT NULL,
    report_json         TEXT         NOT NULL,
    classification_json TEXT,
    passed              INT          NOT NULL,
    total               INT          NOT NULL,
    efficiency          VARCHAR(20)  NOT NULL,
    slope               DOUBLE PRECISION,
    assistant_message   TEXT,
    llm_status          VARCHAR(30)  NOT NULL,
    duration_ms         BIGINT       NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_submission_version UNIQUE (session_id, version)
);

CREATE TABLE observation (
    id                   UUID PRIMARY KEY,
    submission_id        UUID        NOT NULL REFERENCES submission (id),
    type                 VARCHAR(20) NOT NULL,
    line                 INT,
    evidence_input_json  TEXT,
    risk                 TEXT        NOT NULL,
    hints_json           TEXT        NOT NULL,
    status               VARCHAR(20) NOT NULL,
    reject_reason        VARCHAR(30),
    student_output_json  TEXT,
    expected_output_json TEXT,
    leak_retries         INT         NOT NULL DEFAULT 0,
    shown                BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE hint_reveal (
    id             UUID PRIMARY KEY,
    observation_id UUID NOT NULL REFERENCES observation (id),
    level          INT  NOT NULL,
    revealed_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE feedback (
    id             UUID PRIMARY KEY,
    observation_id UUID        NOT NULL REFERENCES observation (id),
    verdict        VARCHAR(10) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE llm_call (
    id            UUID PRIMARY KEY,
    submission_id UUID REFERENCES submission (id),
    purpose       VARCHAR(30)    NOT NULL,
    model         VARCHAR(50)    NOT NULL,
    input_tokens  INT            NOT NULL,
    output_tokens INT            NOT NULL,
    cost_usd      DECIMAL(12, 8) NOT NULL,
    latency_ms    BIGINT         NOT NULL,
    success       BOOLEAN        NOT NULL,
    error         TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_submission_session ON submission (session_id);
CREATE INDEX idx_observation_submission ON observation (submission_id);
CREATE INDEX idx_hint_reveal_observation ON hint_reveal (observation_id);
CREATE INDEX idx_feedback_observation ON feedback (observation_id);
