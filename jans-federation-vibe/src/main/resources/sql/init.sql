-- Database initialization script for Jans Federation Vibe
-- This script creates the necessary tables for the federation application

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
