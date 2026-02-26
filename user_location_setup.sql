-- ============================================
-- User Location Tracking System
-- Find users within 1km radius
-- ============================================

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create users table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create user_locations table to track current positions
CREATE TABLE user_locations (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    location GEOMETRY(Point, 4326) NOT NULL,
    accuracy FLOAT,  -- GPS accuracy in meters
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_user_location UNIQUE(user_id)
);

-- Create spatial index for performance (CRITICAL for location queries)
CREATE INDEX idx_user_locations_geom ON user_locations USING GIST(location);

-- Create index on user_id for faster joins
CREATE INDEX idx_user_locations_user_id ON user_locations(user_id);

-- Create index on updated_at for filtering stale locations
CREATE INDEX idx_user_locations_updated_at ON user_locations(updated_at);

-- ============================================
-- FUNCTION: Update or Insert User Location
-- ============================================
CREATE OR REPLACE FUNCTION update_user_location(
    p_user_id INTEGER,
    p_longitude FLOAT,
    p_latitude FLOAT,
    p_accuracy FLOAT DEFAULT NULL
)
RETURNS TABLE(success BOOLEAN, message TEXT) AS $$
BEGIN
    -- Validate coordinates
    IF p_longitude < -180 OR p_longitude > 180 THEN
        RETURN QUERY SELECT FALSE, 'Invalid longitude: must be between -180 and 180';
        RETURN;
    END IF;
    
    IF p_latitude < -90 OR p_latitude > 90 THEN
        RETURN QUERY SELECT FALSE, 'Invalid latitude: must be between -90 and 90';
        RETURN;
    END IF;
    
    -- Upsert location
    INSERT INTO user_locations (user_id, location, accuracy, updated_at)
    VALUES (
        p_user_id,
        ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326),
        p_accuracy,
        NOW()
    )
    ON CONFLICT (user_id) 
    DO UPDATE SET
        location = ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326),
        accuracy = p_accuracy,
        updated_at = NOW();
    
    RETURN QUERY SELECT TRUE, 'Location updated successfully';
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- FUNCTION: Find Users Within Radius
-- ============================================
CREATE OR REPLACE FUNCTION find_nearby_users(
    p_user_id INTEGER,
    p_radius_meters FLOAT DEFAULT 1000,
    p_max_age_minutes INTEGER DEFAULT 30
)
RETURNS TABLE(
    user_id INTEGER,
    username VARCHAR,
    email VARCHAR,
    distance_meters FLOAT,
    latitude FLOAT,
    longitude FLOAT,
    last_updated TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.id,
        u.username,
        u.email,
        ST_Distance(ul.location::geography, my_loc.location::geography) as distance_meters,
        ST_Y(ul.location) as latitude,
        ST_X(ul.location) as longitude,
        ul.updated_at
    FROM user_locations ul
    JOIN users u ON ul.user_id = u.id
    CROSS JOIN (
        SELECT location 
        FROM user_locations 
        WHERE user_id = p_user_id
    ) my_loc
    WHERE ul.user_id != p_user_id  -- Exclude self
        AND ul.updated_at > NOW() - (p_max_age_minutes || ' minutes')::INTERVAL  -- Only recent locations
        AND ST_DWithin(
            ul.location::geography,
            my_loc.location::geography,
            p_radius_meters
        )
    ORDER BY distance_meters ASC;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- FUNCTION: Find Users Near Coordinates
-- ============================================
CREATE OR REPLACE FUNCTION find_users_near_point(
    p_longitude FLOAT,
    p_latitude FLOAT,
    p_radius_meters FLOAT DEFAULT 1000,
    p_max_age_minutes INTEGER DEFAULT 30
)
RETURNS TABLE(
    user_id INTEGER,
    username VARCHAR,
    email VARCHAR,
    distance_meters FLOAT,
    latitude FLOAT,
    longitude FLOAT,
    last_updated TIMESTAMP
) AS $$
DECLARE
    search_point GEOGRAPHY;
BEGIN
    -- Create search point
    search_point := ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography;
    
    RETURN QUERY
    SELECT 
        u.id,
        u.username,
        u.email,
        ST_Distance(ul.location::geography, search_point) as distance_meters,
        ST_Y(ul.location) as latitude,
        ST_X(ul.location) as longitude,
        ul.updated_at
    FROM user_locations ul
    JOIN users u ON ul.user_id = u.id
    WHERE ul.updated_at > NOW() - (p_max_age_minutes || ' minutes')::INTERVAL
        AND ST_DWithin(ul.location::geography, search_point, p_radius_meters)
    ORDER BY distance_meters ASC;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Sample Data (for testing)
-- ============================================

-- Insert sample users
INSERT INTO users (username, email) VALUES
    ('alice', 'alice@example.com'),
    ('bob', 'bob@example.com'),
    ('charlie', 'charlie@example.com'),
    ('diana', 'diana@example.com'),
    ('eve', 'eve@example.com')
ON CONFLICT (username) DO NOTHING;

-- Insert sample locations (around a central point in New York City)
-- Central point: Times Square (40.7580° N, 73.9855° W)

-- Alice at Times Square
SELECT update_user_location(
    (SELECT id FROM users WHERE username = 'alice'),
    -73.9855, 40.7580, 10.0
);

-- Bob 500m north
SELECT update_user_location(
    (SELECT id FROM users WHERE username = 'bob'),
    -73.9855, 40.7625, 15.0
);

-- Charlie 800m east
SELECT update_user_location(
    (SELECT id FROM users WHERE username = 'charlie'),
    -73.9755, 40.7580, 12.0
);

-- Diana 1.5km south (outside 1km radius)
SELECT update_user_location(
    (SELECT id FROM users WHERE username = 'diana'),
    -73.9855, 40.7445, 20.0
);

-- Eve 2km west (outside 1km radius)
SELECT update_user_location(
    (SELECT id FROM users WHERE username = 'eve'),
    -74.0055, 40.7580, 18.0
);

-- ============================================
-- Example Queries
-- ============================================

-- 1. Find users within 1km of Alice
SELECT * FROM find_nearby_users(
    (SELECT id FROM users WHERE username = 'alice'),
    1000  -- 1km in meters
);

-- 2. Find users within 1km of a specific coordinate
SELECT * FROM find_users_near_point(
    -73.9855,  -- longitude
    40.7580,   -- latitude
    1000       -- 1km radius
);

-- 3. Get current location of a user
SELECT 
    u.username,
    ST_Y(ul.location) as latitude,
    ST_X(ul.location) as longitude,
    ul.accuracy,
    ul.updated_at
FROM user_locations ul
JOIN users u ON ul.user_id = u.id
WHERE u.username = 'alice';

-- 4. Count users in area
SELECT COUNT(*) as users_in_area
FROM find_users_near_point(-73.9855, 40.7580, 1000);

-- 5. Find all users with their last known locations
SELECT 
    u.id,
    u.username,
    ST_Y(ul.location) as latitude,
    ST_X(ul.location) as longitude,
    ul.accuracy,
    ul.updated_at,
    EXTRACT(EPOCH FROM (NOW() - ul.updated_at))/60 as minutes_ago
FROM users u
LEFT JOIN user_locations ul ON u.id = ul.user_id
ORDER BY ul.updated_at DESC NULLS LAST;
