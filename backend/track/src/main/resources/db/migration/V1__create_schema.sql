CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
    id      BIGINT      NOT NULL AUTO_INCREMENT,
    user_id BIGINT      NULL,              -- NULL = system default category
    name    VARCHAR(50) NOT NULL,
    color   VARCHAR(7)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE expenses (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    category_id  BIGINT        NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    description  VARCHAR(255)  NULL,
    expense_date DATE          NOT NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_expenses_amount CHECK (amount > 0),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT,
    INDEX idx_expenses_user_date (user_id, expense_date),
    INDEX idx_expenses_user_category (user_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_suggestions (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    period_start  DATE        NOT NULL,
    period_end    DATE        NOT NULL,
    data_hash     VARCHAR(64) NOT NULL,
    response_json JSON        NOT NULL,
    source        VARCHAR(20) NOT NULL,
    model         VARCHAR(50) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_ai_source CHECK (source IN ('GEMINI', 'FALLBACK')),
    CONSTRAINT fk_ai_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_ai_user_created (user_id, created_at),
    INDEX idx_ai_user_hash (user_id, data_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;