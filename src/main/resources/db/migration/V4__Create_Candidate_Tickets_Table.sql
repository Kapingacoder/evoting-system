-- Create candidate_tickets table for President & Vice President tickets
CREATE TABLE IF NOT EXISTS candidate_tickets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    president_name VARCHAR(255) NOT NULL,
    president_party VARCHAR(255) NOT NULL,
    president_photo_url VARCHAR(500),
    vice_president_name VARCHAR(255) NOT NULL,
    vice_president_party VARCHAR(255) NOT NULL,
    vice_president_photo_url VARCHAR(500),
    vote_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP
);

-- Add created_at timestamp if not exists (for existing records)
ALTER TABLE candidate_tickets ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

