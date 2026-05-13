-- Fix candidate_tickets table - add missing columns
-- First drop the existing table if it has wrong schema
DROP TABLE IF EXISTS candidate_tickets;

-- Recreate the table with correct schema
CREATE TABLE candidate_tickets (
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

