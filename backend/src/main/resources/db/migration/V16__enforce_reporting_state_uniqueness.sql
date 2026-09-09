CREATE UNIQUE INDEX IF NOT EXISTS uq_str_gifim_registration
    ON suspicious_activity_reports(gifim_registration_number)
    WHERE gifim_registration_number IS NOT NULL;
