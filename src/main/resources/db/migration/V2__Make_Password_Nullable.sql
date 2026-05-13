-- Make password column nullable
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
