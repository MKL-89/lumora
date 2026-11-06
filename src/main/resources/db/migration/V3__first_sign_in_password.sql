-- Accounts an administrator creates (or resets) carry a temporary password. The
-- holder must choose their own before they can use anything else.
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
