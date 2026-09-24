-- Email OTP support for registration
-- Make phone nullable on otp_records (email OTPs won't have a phone)
ALTER TABLE otp_records ALTER COLUMN phone DROP NOT NULL;

-- Add email column for email-based OTPs
ALTER TABLE otp_records ADD COLUMN IF NOT EXISTS email VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_otp_email ON otp_records(email);

-- Track email verification on users
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
