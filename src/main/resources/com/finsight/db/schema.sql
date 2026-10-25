-- FinSight database schema (SQLite)
--
-- Money is stored as whole paise/cents in INTEGER columns so sums are exact.
-- Income and expenses share one base table, with a detail table for the
-- columns only one of them has (class table inheritance). This mirrors the
-- Transaction / Income / Expense classes in the Java model.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS category (
    category_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL CHECK (length(trim(name)) > 0),
    type         TEXT    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    color_hex    TEXT    NOT NULL CHECK (color_hex GLOB '#[0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F]'),
    UNIQUE (name COLLATE NOCASE, type)
);

CREATE TABLE IF NOT EXISTS txn (
    txn_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    type          TEXT    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount_cents  INTEGER NOT NULL CHECK (amount_cents > 0),
    txn_date      TEXT    NOT NULL CHECK (date(txn_date) = txn_date),
    category_id   INTEGER NOT NULL REFERENCES category (category_id) ON DELETE RESTRICT,
    description   TEXT    NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS income_detail (
    txn_id  INTEGER PRIMARY KEY REFERENCES txn (txn_id) ON DELETE CASCADE,
    source  TEXT    NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS expense_detail (
    txn_id          INTEGER PRIMARY KEY REFERENCES txn (txn_id) ON DELETE CASCADE,
    payment_method  TEXT    NOT NULL CHECK (payment_method IN ('CASH', 'CARD', 'UPI', 'BANK_TRANSFER'))
);

CREATE TABLE IF NOT EXISTS budget (
    budget_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id  INTEGER NOT NULL REFERENCES category (category_id) ON DELETE CASCADE,
    month        TEXT    NOT NULL CHECK (month GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]'),
    limit_cents  INTEGER NOT NULL CHECK (limit_cents > 0),
    UNIQUE (category_id, month)
);

CREATE INDEX IF NOT EXISTS idx_txn_date     ON txn (txn_date);
CREATE INDEX IF NOT EXISTS idx_txn_category ON txn (category_id);

-- A transaction's type must match its category's type.
CREATE TRIGGER IF NOT EXISTS trg_txn_category_type_insert
BEFORE INSERT ON txn
WHEN NEW.type <> (SELECT type FROM category WHERE category_id = NEW.category_id)
BEGIN
    SELECT RAISE(ABORT, 'Transaction type does not match category type');
END;

CREATE TRIGGER IF NOT EXISTS trg_txn_category_type_update
BEFORE UPDATE OF type, category_id ON txn
WHEN NEW.type <> (SELECT type FROM category WHERE category_id = NEW.category_id)
BEGIN
    SELECT RAISE(ABORT, 'Transaction type does not match category type');
END;

-- Budgets only make sense on expense categories.
CREATE TRIGGER IF NOT EXISTS trg_budget_expense_only
BEFORE INSERT ON budget
WHEN (SELECT type FROM category WHERE category_id = NEW.category_id) <> 'EXPENSE'
BEGIN
    SELECT RAISE(ABORT, 'Budgets can only be set on expense categories');
END;

-- Income, expense and net per month.
CREATE VIEW IF NOT EXISTS v_monthly_summary AS
SELECT substr(txn_date, 1, 7)                                          AS month,
       COALESCE(SUM(CASE WHEN type = 'INCOME'  THEN amount_cents END), 0) AS income_cents,
       COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount_cents END), 0) AS expense_cents,
       COALESCE(SUM(CASE WHEN type = 'INCOME'  THEN amount_cents ELSE -amount_cents END), 0) AS net_cents,
       COUNT(*)                                                        AS txn_count
FROM txn
GROUP BY substr(txn_date, 1, 7);

-- Total per category per month.
CREATE VIEW IF NOT EXISTS v_category_totals AS
SELECT substr(t.txn_date, 1, 7) AS month,
       c.category_id,
       c.name,
       c.type,
       c.color_hex,
       SUM(t.amount_cents)      AS total_cents,
       COUNT(*)                 AS txn_count
FROM txn t
JOIN category c ON c.category_id = t.category_id
GROUP BY month, c.category_id;

-- Each budget next to what has been spent against it.
CREATE VIEW IF NOT EXISTS v_budget_status AS
SELECT b.budget_id,
       b.category_id,
       b.month,
       b.limit_cents,
       COALESCE(SUM(t.amount_cents), 0) AS spent_cents
FROM budget b
LEFT JOIN txn t
       ON t.category_id = b.category_id
      AND substr(t.txn_date, 1, 7) = b.month
      AND t.type = 'EXPENSE'
GROUP BY b.budget_id;
