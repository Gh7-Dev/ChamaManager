-- ============================================================
-- Chama Manager - database setup (MySQL 5.7+ / 8.x / MariaDB 10.2+)
-- Re-runnable: drops and recreates the tables (WIPES ALL DATA).
-- ============================================================

CREATE DATABASE IF NOT EXISTS chama_manager
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chama_manager;

-- Drop in reverse dependency order so foreign keys don't block it
DROP TABLE IF EXISTS repayments;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS contributions;
DROP TABLE IF EXISTS members;

-- ------------------------------------------------------------
-- 1. members
-- ------------------------------------------------------------
CREATE TABLE members (
    member_id     INT           NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100)  NOT NULL,
    username      VARCHAR(50)   NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    phone         VARCHAR(20)   NOT NULL,
    role          VARCHAR(20)   NOT NULL DEFAULT 'MEMBER',
    date_joined   DATE          NOT NULL,
    PRIMARY KEY (member_id),
    UNIQUE KEY uq_members_username (username),
    CONSTRAINT chk_members_role CHECK (role IN ('MEMBER', 'TREASURER'))
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 2. contributions
-- ------------------------------------------------------------
CREATE TABLE contributions (
    contribution_id INT            NOT NULL AUTO_INCREMENT,
    member_id       INT            NOT NULL,
    amount          DECIMAL(12,2)  NOT NULL,
    date_paid       DATE           NOT NULL,
    period          VARCHAR(20)    NOT NULL,   -- format used here: 'YYYY-MM'
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (contribution_id),
    KEY idx_contrib_member (member_id),
    KEY idx_contrib_period (period),
    CONSTRAINT fk_contrib_member FOREIGN KEY (member_id)
        REFERENCES members (member_id),
    CONSTRAINT chk_contrib_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT chk_contrib_amount CHECK (amount > 0)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 3. loans
--    interest_rate, date_issued, due_date are NULLable because a
--    PENDING/REJECTED loan has none of them yet (the treasurer
--    sets them at approval/issue time).
-- ------------------------------------------------------------
CREATE TABLE loans (
    loan_id       INT            NOT NULL AUTO_INCREMENT,
    member_id     INT            NOT NULL,
    principal     DECIMAL(12,2)  NOT NULL,
    interest_rate DECIMAL(5,4)   NULL,         -- 0.1000 = 10%
    date_issued   DATE           NULL,
    due_date      DATE           NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (loan_id),
    KEY idx_loans_member (member_id),
    KEY idx_loans_status (status),
    CONSTRAINT fk_loans_member FOREIGN KEY (member_id)
        REFERENCES members (member_id),
    CONSTRAINT chk_loans_status CHECK (status IN
        ('PENDING', 'APPROVED', 'REJECTED', 'ACTIVE', 'PAID', 'DEFAULTED')),
    CONSTRAINT chk_loans_principal CHECK (principal > 0)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 4. repayments
-- ------------------------------------------------------------
CREATE TABLE repayments (
    repayment_id INT            NOT NULL AUTO_INCREMENT,
    loan_id      INT            NOT NULL,
    amount_paid  DECIMAL(12,2)  NOT NULL,
    date_paid    DATE           NOT NULL,
    PRIMARY KEY (repayment_id),
    KEY idx_repay_loan (loan_id),
    CONSTRAINT fk_repay_loan FOREIGN KEY (loan_id)
        REFERENCES loans (loan_id),
    CONSTRAINT chk_repay_amount CHECK (amount_paid > 0)
) ENGINE=InnoDB;

-- ============================================================
-- DEMO DATA
-- All demo accounts use the password:  password123
-- Hashes are PBKDF2WithHmacSHA256 (600,000 iterations, random 16-byte
-- salt, 256-bit key), stored as  iterations:salt_base64:hash_base64
-- This matches PasswordUtil.hashPassword() in the team guide.
-- ============================================================

INSERT INTO members (full_name, username, password_hash, phone, role, date_joined) VALUES
('Grace Achieng',   'treasurer', '600000:QdhMkAJoae3kJj4+aWXXVg==:+iEvCWScmqXiMJ+4KG3EUo2mwXXMI58onv4VcgO0C0k=', '0712345001', 'TREASURER', '2026-01-10'),
('Brian Otieno',    'botieno',   '600000:rGpM8kqQJH4cKx7Py3stpw==:xSQPQEvTbBun46iE1HLAVv825o8ips61w65TNx6RtK8=', '0712345002', 'MEMBER',    '2026-01-10'),
('Mercy Wanjiru',   'mwanjiru',  '600000:EfHpuNW1sFtEg3qiYNPK0Q==:aSx3K6PMdD90sCBg7FjlOuc0mA+24eK8lBlMTfHn/1E=', '0712345003', 'MEMBER',    '2026-01-10'),
('Kevin Odhiambo',  'kodhiambo', '600000:xJAqgf72PVwFD5MSl8icKw==:e5XN92XGZnDaqYsIHKOahL5SD8grYQ2GHe4whZ3tGkQ=', '0712345004', 'MEMBER',    '2026-02-01'),
('Faith Nyambura',  'fnyambura', '600000:egNtajy7TsoMgnegm9mOQw==:8TY1x63CMFEgM+PmdDU26MHYJmnXzRCMoQZJjKiiQI8=', '0712345005', 'MEMBER',    '2026-02-01'),
('Samuel Kiprop',   'skiprop',   '600000:OGo0F5sf0IDi5urSXQNqyw==:hSVLQ4kvZJ8ErrJMnXkK+NiYe/kF3imEnKVzw7pisDs=', '0712345006', 'MEMBER',    '2026-03-15');

-- Contributions: monthly KES 2,000 for Jul-Sep 2026.
-- Mix of CONFIRMED / PENDING / REJECTED. Samuel has none for Sep (arrears test).
INSERT INTO contributions (member_id, amount, date_paid, period, status) VALUES
(1, 2000.00, '2026-07-05', '2026-07', 'CONFIRMED'),
(2, 2000.00, '2026-07-06', '2026-07', 'CONFIRMED'),
(3, 2000.00, '2026-07-05', '2026-07', 'CONFIRMED'),
(4, 2000.00, '2026-07-08', '2026-07', 'CONFIRMED'),
(5, 2000.00, '2026-07-07', '2026-07', 'CONFIRMED'),
(6, 2000.00, '2026-07-09', '2026-07', 'CONFIRMED'),
(1, 2000.00, '2026-08-05', '2026-08', 'CONFIRMED'),
(2, 2000.00, '2026-08-04', '2026-08', 'CONFIRMED'),
(3, 2000.00, '2026-08-06', '2026-08', 'CONFIRMED'),
(4, 2000.00, '2026-08-10', '2026-08', 'CONFIRMED'),
(5, 2000.00, '2026-08-07', '2026-08', 'CONFIRMED'),
(6, 2000.00, '2026-08-12', '2026-08', 'CONFIRMED'),
(1, 2000.00, '2026-09-05', '2026-09', 'CONFIRMED'),
(2, 2000.00, '2026-09-05', '2026-09', 'CONFIRMED'),
(3, 2000.00, '2026-09-06', '2026-09', 'CONFIRMED'),
(4, 2000.00, '2026-09-28', '2026-09', 'PENDING'),    -- awaiting treasurer
(5, 1500.00, '2026-09-29', '2026-09', 'REJECTED');   -- wrong amount / not received

-- Loans in different states
INSERT INTO loans (member_id, principal, interest_rate, date_issued, due_date, status) VALUES
(2, 10000.00, 0.1000, '2026-07-15', '2026-10-15', 'ACTIVE'),     -- on track, partly repaid
(3,  8000.00, 0.1200, '2026-06-01', '2026-09-01', 'DEFAULTED'),  -- past due, unpaid balance
(5,  5000.00, 0.1000, '2026-05-10', '2026-08-10', 'PAID'),       -- fully repaid
(4,  6000.00, NULL,   NULL,         NULL,         'PENDING'),    -- awaiting approval
(6,  3000.00, NULL,   NULL,         NULL,         'REJECTED');   -- rejected

-- Repayments
INSERT INTO repayments (loan_id, amount_paid, date_paid) VALUES
(1, 4000.00, '2026-08-20'),
(1, 3000.00, '2026-09-20'),
(2, 2000.00, '2026-07-15'),
(3, 2750.00, '2026-06-10'),
(3, 2750.00, '2026-08-05');

-- ------------------------------------------------------------
-- Quick sanity check (should show 6 / 17 / 5 / 5)
-- ------------------------------------------------------------
SELECT (SELECT COUNT(*) FROM members)       AS members,
       (SELECT COUNT(*) FROM contributions) AS contributions,
       (SELECT COUNT(*) FROM loans)         AS loans,
       (SELECT COUNT(*) FROM repayments)    AS repayments;
