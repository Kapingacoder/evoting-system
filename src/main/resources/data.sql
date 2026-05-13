-- Insert admin user with password 'admin123' (BCrypt hashed)
-- The hashed password for 'admin123' is: $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a

-- First, make sure the roles exist
INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN'), (2, 'ROLE_VOTER')
ON CONFLICT (id) DO NOTHING;

-- Then insert the admin user
-- Note: The password is already hashed (it's 'admin123' hashed with BCrypt)
INSERT INTO users (username, password, email, first_name, last_name, role_id)
SELECT 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'admin@example.com', 'System', 'Administrator', 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
