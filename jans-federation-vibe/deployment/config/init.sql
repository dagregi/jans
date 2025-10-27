-- Database initialization script for Jans Federation Vibe
-- This script creates the necessary tables for the federation application

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS federation;

-- Use the federation database
\c federation;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create entity_configurations table
CREATE TABLE IF NOT EXISTS entity_configurations (
    id SERIAL PRIMARY KEY,
    entity_id VARCHAR(255) UNIQUE NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    audience VARCHAR(255),
    expiration_time BIGINT,
    issued_at BIGINT,
    jwt_id VARCHAR(255),
    authority_hints TEXT[],
    jwks JSONB,
    metadata JSONB,
    metadata_policy JSONB,
    critical TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create trust_marks table
CREATE TABLE IF NOT EXISTS trust_marks (
    id SERIAL PRIMARY KEY,
    entity_id VARCHAR(255) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    audience VARCHAR(255),
    expiration_time BIGINT,
    issued_at BIGINT,
    jwt_id VARCHAR(255),
    trust_mark_id VARCHAR(255),
    trust_mark TEXT,
    reference VARCHAR(255),
    mark JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (entity_id) REFERENCES entity_configurations(entity_id)
);

-- Create trust_mark_issuers table
CREATE TABLE IF NOT EXISTS trust_mark_issuers (
    id SERIAL PRIMARY KEY,
    issuer VARCHAR(255) UNIQUE NOT NULL,
    subject VARCHAR(255) NOT NULL,
    audience VARCHAR(255),
    expiration_time BIGINT,
    issued_at BIGINT,
    jwt_id VARCHAR(255),
    trust_mark_issuers TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create trust_mark_profiles table
CREATE TABLE IF NOT EXISTS trust_mark_profiles (
    id SERIAL PRIMARY KEY,
    profile_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    trust_mark TEXT,
    reference VARCHAR(255),
    mark JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create federation_metadata table
CREATE TABLE IF NOT EXISTS federation_metadata (
    id SERIAL PRIMARY KEY,
    issuer VARCHAR(255) UNIQUE NOT NULL,
    jwks_uri VARCHAR(255),
    authority_hints TEXT[],
    signed_jwks_uri VARCHAR(255),
    federation_metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_entity_configurations_entity_id ON entity_configurations(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_configurations_issuer ON entity_configurations(issuer);
CREATE INDEX IF NOT EXISTS idx_entity_configurations_subject ON entity_configurations(subject);

CREATE INDEX IF NOT EXISTS idx_trust_marks_entity_id ON trust_marks(entity_id);
CREATE INDEX IF NOT EXISTS idx_trust_marks_issuer ON trust_marks(issuer);
CREATE INDEX IF NOT EXISTS idx_trust_marks_subject ON trust_marks(subject);
CREATE INDEX IF NOT EXISTS idx_trust_marks_trust_mark_id ON trust_marks(trust_mark_id);

CREATE INDEX IF NOT EXISTS idx_trust_mark_issuers_issuer ON trust_mark_issuers(issuer);
CREATE INDEX IF NOT EXISTS idx_trust_mark_issuers_subject ON trust_mark_issuers(subject);

CREATE INDEX IF NOT EXISTS idx_trust_mark_profiles_profile_id ON trust_mark_profiles(profile_id);

CREATE INDEX IF NOT EXISTS idx_federation_metadata_issuer ON federation_metadata(issuer);

-- Insert sample federation metadata
INSERT INTO federation_metadata (issuer, jwks_uri, authority_hints, signed_jwks_uri, federation_metadata) 
VALUES ('https://federation.example.com', 'https://federation.example.com/jwks', 
        ARRAY['https://authority.example.com'], 'https://federation.example.com/signed-jwks',
        '{"federation_name": "Example Federation", "version": "1.0", "contact": "admin@federation.example.com"}')
ON CONFLICT (issuer) DO NOTHING;

-- Insert sample trust mark issuers
INSERT INTO trust_mark_issuers (issuer, subject, audience, trust_mark_issuers, expiration_time, issued_at, jwt_id)
VALUES 
    ('https://trustmark.example.com', 'https://trustmark.example.com', 'federation',
     ARRAY['https://trustmark.example.com'], EXTRACT(EPOCH FROM NOW() + INTERVAL '1 year'), 
     EXTRACT(EPOCH FROM NOW()), 'trustmark-issuer-1'),
    ('https://authority.example.com', 'https://authority.example.com', 'federation',
     ARRAY['https://authority.example.com'], EXTRACT(EPOCH FROM NOW() + INTERVAL '1 year'),
     EXTRACT(EPOCH FROM NOW()), 'authority-issuer-1')
ON CONFLICT (issuer) DO NOTHING;

-- Insert sample trust mark profiles
INSERT INTO trust_mark_profiles (profile_id, name, description, trust_mark, reference, mark)
VALUES 
    ('basic-trust', 'Basic Trust Profile', 'Basic trust profile for federation members',
     'https://trustmark.example.com/trustmarks/basic', 'https://trustmark.example.com/trustmarks/basic',
     '{"level": "basic", "requirements": ["email_verification"], "validity_period": 365}'),
    ('advanced-trust', 'Advanced Trust Profile', 'Advanced trust profile for verified entities',
     'https://trustmark.example.com/trustmarks/advanced', 'https://trustmark.example.com/trustmarks/advanced',
     '{"level": "advanced", "requirements": ["email_verification", "domain_verification"], "validity_period": 730}'),
    ('enterprise-trust', 'Enterprise Trust Profile', 'Enterprise trust profile for organizations',
     'https://trustmark.example.com/trustmarks/enterprise', 'https://trustmark.example.com/trustmarks/enterprise',
     '{"level": "enterprise", "requirements": ["domain_verification", "legal_verification"], "validity_period": 1095}')
ON CONFLICT (profile_id) DO NOTHING;

-- Insert sample entity configurations
INSERT INTO entity_configurations (entity_id, issuer, subject, audience, expiration_time, issued_at, jwt_id, authority_hints, jwks, metadata)
VALUES 
    ('https://op.example.com', 'https://op.example.com', 'https://op.example.com', 'federation',
     EXTRACT(EPOCH FROM NOW() + INTERVAL '1 year'), EXTRACT(EPOCH FROM NOW()), 'op-config-1',
     ARRAY['https://authority.example.com'],
     '{"keys": [{"kty": "RSA", "kid": "op-key-1", "use": "sig", "alg": "RS256"}]}',
     '{"openid_provider": {"issuer": "https://op.example.com", "authorization_endpoint": "https://op.example.com/auth", "token_endpoint": "https://op.example.com/token", "userinfo_endpoint": "https://op.example.com/userinfo", "jwks_uri": "https://op.example.com/jwks"}}'),
    ('https://rp.example.com', 'https://rp.example.com', 'https://rp.example.com', 'federation',
     EXTRACT(EPOCH FROM NOW() + INTERVAL '1 year'), EXTRACT(EPOCH FROM NOW()), 'rp-config-1',
     ARRAY['https://authority.example.com'],
     '{"keys": [{"kty": "RSA", "kid": "rp-key-1", "use": "sig", "alg": "RS256"}]}',
     '{"openid_relying_party": {"client_id": "rp-client-1", "redirect_uris": ["https://rp.example.com/callback"], "response_types": ["code"], "grant_types": ["authorization_code"]}}')
ON CONFLICT (entity_id) DO NOTHING;

-- Insert sample trust marks
INSERT INTO trust_marks (entity_id, issuer, subject, audience, expiration_time, issued_at, jwt_id, trust_mark_id, trust_mark, reference, mark)
VALUES 
    ('https://op.example.com', 'https://trustmark.example.com', 'https://op.example.com', 'https://op.example.com',
     EXTRACT(EPOCH FROM NOW() + INTERVAL '1 year'), EXTRACT(EPOCH FROM NOW()), 'trustmark-1',
     'basic-trust', 'https://trustmark.example.com/trustmarks/basic', 'https://trustmark.example.com/trustmarks/basic',
     '{"level": "basic", "issued_by": "https://trustmark.example.com", "valid_until": "' || (NOW() + INTERVAL '1 year')::text || '"}'),
    ('https://rp.example.com', 'https://trustmark.example.com', 'https://rp.example.com', 'https://rp.example.com',
     EXTRACT(EPOCH FROM NOW() + INTERVAL '1 year'), EXTRACT(EPOCH FROM NOW()), 'trustmark-2',
     'basic-trust', 'https://trustmark.example.com/trustmarks/basic', 'https://trustmark.example.com/trustmarks/basic',
     '{"level": "basic", "issued_by": "https://trustmark.example.com", "valid_until": "' || (NOW() + INTERVAL '1 year')::text || '"}')
ON CONFLICT DO NOTHING;

-- Create a view for easy querying of active trust marks
CREATE OR REPLACE VIEW active_trust_marks AS
SELECT 
    tm.*,
    ec.issuer as entity_issuer,
    ec.subject as entity_subject,
    tmp.name as profile_name,
    tmp.description as profile_description
FROM trust_marks tm
JOIN entity_configurations ec ON tm.entity_id = ec.entity_id
LEFT JOIN trust_mark_profiles tmp ON tm.trust_mark_id = tmp.profile_id
WHERE tm.expiration_time > EXTRACT(EPOCH FROM NOW());

-- Create a view for federation statistics
CREATE OR REPLACE VIEW federation_stats AS
SELECT 
    (SELECT COUNT(*) FROM entity_configurations) as total_entities,
    (SELECT COUNT(*) FROM trust_marks WHERE expiration_time > EXTRACT(EPOCH FROM NOW())) as active_trust_marks,
    (SELECT COUNT(*) FROM trust_mark_issuers) as trust_mark_issuers,
    (SELECT COUNT(*) FROM trust_mark_profiles) as trust_mark_profiles;

-- Insert additional sample data for testing
INSERT INTO entity_configurations (entity_id, issuer, subject, audience, expiration_time, issued_at, jwt_id, authority_hints, jwks, metadata)
VALUES 
    ('https://test-op.example.com', 'https://test-op.example.com', 'https://test-op.example.com', 'federation',
     EXTRACT(EPOCH FROM NOW() + INTERVAL '6 months'), EXTRACT(EPOCH FROM NOW()), 'test-op-config-1',
     ARRAY['https://authority.example.com'],
     '{"keys": [{"kty": "RSA", "kid": "test-op-key-1", "use": "sig", "alg": "RS256"}]}',
     '{"openid_provider": {"issuer": "https://test-op.example.com", "authorization_endpoint": "https://test-op.example.com/auth", "token_endpoint": "https://test-op.example.com/token", "userinfo_endpoint": "https://test-op.example.com/userinfo", "jwks_uri": "https://test-op.example.com/jwks"}}')
ON CONFLICT (entity_id) DO NOTHING;

-- Insert test trust marks
INSERT INTO trust_marks (entity_id, issuer, subject, audience, expiration_time, issued_at, jwt_id, trust_mark_id, trust_mark, reference, mark)
VALUES 
    ('https://test-op.example.com', 'https://trustmark.example.com', 'https://test-op.example.com', 'https://test-op.example.com',
     EXTRACT(EPOCH FROM NOW() + INTERVAL '6 months'), EXTRACT(EPOCH FROM NOW()), 'test-trustmark-1',
     'advanced-trust', 'https://trustmark.example.com/trustmarks/advanced', 'https://trustmark.example.com/trustmarks/advanced',
     '{"level": "advanced", "issued_by": "https://trustmark.example.com", "valid_until": "' || (NOW() + INTERVAL '6 months')::text || '"}')
ON CONFLICT DO NOTHING;
