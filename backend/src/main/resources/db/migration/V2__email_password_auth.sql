-- V2: switch from USC SSO to email/password with email verification.
-- Adds first/last name and the columns required to issue and validate
-- one-time email verification codes. Existing rows (if any) get NULL
-- so the migration is non-destructive.

ALTER TABLE Users
    ADD COLUMN firstName                 VARCHAR(100) NULL AFTER password,
    ADD COLUMN lastName                  VARCHAR(100) NULL AFTER firstName,
    ADD COLUMN verificationCode          VARCHAR(16)  NULL,
    ADD COLUMN verificationCodeExpiresAt DATETIME     NULL;
