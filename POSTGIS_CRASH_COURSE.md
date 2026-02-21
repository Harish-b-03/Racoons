# PostGIS Crash Course

## Table of Contents
1. [What is PostGIS?](#what-is-postgis)
2. [Setup & Installation](#setup--installation)
3. [Core Concepts](#core-concepts)
4. [Geometry Types](#geometry-types)
5. [Essential Functions](#essential-functions)
6. [Practical Examples](#practical-examples)
7. [Spatial Queries](#spatial-queries)
8. [Performance & Indexing](#performance--indexing)
9. [Real-World Use Cases](#real-world-use-cases)

---

## What is PostGIS?

PostGIS is a spatial database extension for PostgreSQL that adds support for geographic objects, allowing location queries to be run in SQL.

**Key Features:**
- Store geometric and geographic data types
- Perform spatial queries (distance, intersection, containment)
- Spatial indexing for performance
- Support for coordinate systems and projections
- Raster data support
- Topology support

---

## Setup & Installation

### Using Docker (Your Current Setup)
```bash
# Start PostGIS container
docker compose up -d

# Connect to database
docker exec -it postgis psql -U admin -d spatial_db
```

### Enable PostGIS Extension
```sql
-- Enable PostGIS
CREATE EXTENSION postgis;

-- Check version
SELECT PostGIS_Version();

-- Enable additional extensions (optional)
CREATE EXTENSION postgis_topology;
CREATE EXTENSION postgis_raster;
```

---

## Core Concepts

### 1. **Geometry vs Geography**
- **Geometry**: Planar (flat earth) coordinates, faster, uses Cartesian math
- **Geography**: Spherical (round earth) coordinates, more accurate for global data

### 2. **Coordinate Systems (SRID)**
- **SRID 4326**: WGS84 (GPS coordinates) - lat/lon
- **SRID 3857**: Web Mercator (used by Google Maps, OpenStreetMap)
- **SRID 0**: No spatial reference

### 3. **Well-Known Text (WKT)**
Human-readable format for geometries:
```
POINT(0 0)
LINESTRING(0 0, 1 1, 2 2)
POLYGON((0 0, 4 0, 4 4, 0 4, 0 0))
```

---

## Geometry Types

### Basic Types

| Type | Description | Example |
|------|-------------|---------|
| **POINT** | Single location | `POINT(30 10)` |
| **LINESTRING** | Connected line segments | `LINESTRING(0 0, 1 1, 2 2)` |
| **POLYGON** | Closed area | `POLYGON((0 0, 4 0, 4 4, 0 4, 0 0))` |
| **MULTIPOINT** | Collection of points | `MULTIPOINT((0 0), (1 1))` |
| **MULTILINESTRING** | Collection of lines | `MULTILINESTRING((0 0, 1 1), (2 2, 3 3))` |
| **MULTIPOLYGON** | Collection of polygons | `MULTIPOLYGON(((0 0, 4 0, 4 4, 0 4, 0 0)))` |
| **GEOMETRYCOLLECTION** | Mixed geometry types | `GEOMETRYCOLLECTION(POINT(0 0), LINESTRING(0 0, 1 1))` |

---

## Essential Functions

### Creating Geometries

```sql
-- From WKT
SELECT ST_GeomFromText('POINT(0 0)', 4326);

-- From coordinates
SELECT ST_MakePoint(longitude, latitude);

-- From GeoJSON
SELECT ST_GeomFromGeoJSON('{"type":"Point","coordinates":[0,0]}');
```

### Geometry Properties

```sql
-- Get geometry type
SELECT ST_GeometryType(geom);

-- Get SRID
SELECT ST_SRID(geom);

-- Get coordinates
SELECT ST_X(geom), ST_Y(geom);  -- For points

-- Get area (in square units)
SELECT ST_Area(geom);

-- Get perimeter/length
SELECT ST_Perimeter(geom);  -- For polygons
SELECT ST_Length(geom);     -- For linestrings
```

### Spatial Relationships

```sql
-- Distance between geometries
SELECT ST_Distance(geom1, geom2);

-- Check if geometries intersect
SELECT ST_Intersects(geom1, geom2);

-- Check if one contains another
SELECT ST_Contains(geom1, geom2);

-- Check if geometries touch
SELECT ST_Touches(geom1, geom2);

-- Check if one is within another
SELECT ST_Within(geom1, geom2);

-- Check if geometries overlap
SELECT ST_Overlaps(geom1, geom2);
```

### Geometry Processing

```sql
-- Buffer (create area around geometry)
SELECT ST_Buffer(geom, distance);

-- Intersection
SELECT ST_Intersection(geom1, geom2);

-- Union
SELECT ST_Union(geom1, geom2);

-- Difference
SELECT ST_Difference(geom1, geom2);

-- Centroid
SELECT ST_Centroid(geom);

-- Simplify (reduce vertices)
SELECT ST_Simplify(geom, tolerance);
```

---

## Practical Examples

### Example 1: Create a Locations Table

```sql
-- Create table
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    location GEOMETRY(Point, 4326),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Insert data
INSERT INTO locations (name, location) VALUES
    ('Statue of Liberty', ST_SetSRID(ST_MakePoint(-74.0445, 40.6892), 4326)),
    ('Eiffel Tower', ST_SetSRID(ST_MakePoint(2.2945, 48.8584), 4326)),
    ('Sydney Opera House', ST_SetSRID(ST_MakePoint(151.2153, -33.8568), 4326)),
    ('Taj Mahal', ST_SetSRID(ST_MakePoint(78.0421, 27.1751), 4326));

-- Query data
SELECT 
    name,
    ST_X(location) as longitude,
    ST_Y(location) as latitude
FROM locations;
```

### Example 2: Find Nearby Places

```sql
-- Find all locations within 1000km of a point
SELECT 
    name,
    ST_Distance(
        location::geography,
        ST_SetSRID(ST_MakePoint(2.3522, 48.8566), 4326)::geography
    ) / 1000 as distance_km
FROM locations
WHERE ST_DWithin(
    location::geography,
    ST_SetSRID(ST_MakePoint(2.3522, 48.8566), 4326)::geography,
    1000000  -- 1000km in meters
)
ORDER BY distance_km;
```

### Example 3: Create Service Areas

```sql
-- Create table for stores with service areas
CREATE TABLE stores (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    location GEOMETRY(Point, 4326),
    service_area GEOMETRY(Polygon, 4326)
);

-- Insert store with 5km service radius
INSERT INTO stores (name, location, service_area)
VALUES (
    'Downtown Store',
    ST_SetSRID(ST_MakePoint(-73.9857, 40.7484), 4326),
    ST_Buffer(
        ST_SetSRID(ST_MakePoint(-73.9857, 40.7484), 4326)::geography,
        5000  -- 5km in meters
    )::geometry
);

-- Find which stores serve a customer location
SELECT s.name
FROM stores s
WHERE ST_Contains(
    s.service_area,
    ST_SetSRID(ST_MakePoint(-73.9900, 40.7500), 4326)
);
```

### Example 4: Route Analysis

```sql
-- Create roads table
CREATE TABLE roads (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    path GEOMETRY(LineString, 4326)
);

-- Insert a road
INSERT INTO roads (name, path) VALUES (
    'Main Street',
    ST_GeomFromText('LINESTRING(-73.9857 40.7484, -73.9800 40.7500, -73.9750 40.7520)', 4326)
);

-- Calculate road length in kilometers
SELECT 
    name,
    ST_Length(path::geography) / 1000 as length_km
FROM roads;

-- Find roads that intersect with an area
SELECT r.name
FROM roads r
WHERE ST_Intersects(
    r.path,
    ST_MakeEnvelope(-73.99, 40.74, -73.97, 40.76, 4326)
);
```

---

## Spatial Queries

### Bounding Box Queries

```sql
-- Create bounding box
SELECT ST_MakeEnvelope(
    min_lon, min_lat,
    max_lon, max_lat,
    4326
);

-- Find points in bounding box
SELECT * FROM locations
WHERE ST_Within(
    location,
    ST_MakeEnvelope(-75, 40, -73, 42, 4326)
);
```

### Nearest Neighbor Queries

```sql
-- Find 5 nearest locations to a point
SELECT 
    name,
    ST_Distance(
        location::geography,
        ST_SetSRID(ST_MakePoint(0, 0), 4326)::geography
    ) as distance
FROM locations
ORDER BY location <-> ST_SetSRID(ST_MakePoint(0, 0), 4326)
LIMIT 5;
```

### Aggregation Queries

```sql
-- Union all geometries
SELECT ST_Union(location) FROM locations;

-- Collect all points into multipoint
SELECT ST_Collect(location) FROM locations;

-- Get extent (bounding box) of all locations
SELECT ST_Extent(location) FROM locations;
```

---

## Performance & Indexing

### Create Spatial Index

```sql
-- Create GIST index (essential for performance)
CREATE INDEX idx_locations_geom 
ON locations 
USING GIST(location);

-- Analyze table for query planner
ANALYZE locations;
```

### Query Optimization Tips

1. **Always use spatial indexes** with GIST
2. **Use bounding box operators** (`&&`) before expensive functions
3. **Cast to geography** only when needed (it's slower)
4. **Use ST_DWithin** instead of ST_Distance for proximity queries

```sql
-- Good: Uses index first
SELECT * FROM locations
WHERE location && ST_MakeEnvelope(-75, 40, -73, 42, 4326)
  AND ST_Contains(
      ST_MakeEnvelope(-75, 40, -73, 42, 4326),
      location
  );

-- Better: Use ST_DWithin for distance queries
SELECT * FROM locations
WHERE ST_DWithin(
    location::geography,
    ST_SetSRID(ST_MakePoint(0, 0), 4326)::geography,
    1000000
);
```

---

## Real-World Use Cases

### 1. **Delivery Service**
```sql
-- Find delivery drivers within 5km of order
SELECT d.name, d.vehicle_type
FROM drivers d
WHERE d.is_available = true
  AND ST_DWithin(
      d.current_location::geography,
      order.pickup_location::geography,
      5000
  )
ORDER BY d.current_location <-> order.pickup_location
LIMIT 3;
```

### 2. **Real Estate Search**
```sql
-- Find properties within polygon area and price range
SELECT p.address, p.price
FROM properties p
WHERE p.price BETWEEN 200000 AND 500000
  AND ST_Within(
      p.location,
      ST_GeomFromText('POLYGON((...coordinates...))', 4326)
  );
```

### 3. **Store Locator**
```sql
-- Find nearest stores with stock
SELECT 
    s.name,
    s.address,
    ST_Distance(
        s.location::geography,
        ST_SetSRID(ST_MakePoint($lon, $lat), 4326)::geography
    ) / 1000 as distance_km
FROM stores s
JOIN inventory i ON s.id = i.store_id
WHERE i.product_id = $product_id
  AND i.quantity > 0
  AND ST_DWithin(
      s.location::geography,
      ST_SetSRID(ST_MakePoint($lon, $lat), 4326)::geography,
      50000  -- 50km
  )
ORDER BY distance_km
LIMIT 5;
```

### 4. **Geofencing**
```sql
-- Check if user entered/exited a geofence
CREATE TABLE geofences (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    area GEOMETRY(Polygon, 4326),
    alert_on_entry BOOLEAN,
    alert_on_exit BOOLEAN
);

-- Check geofence triggers
SELECT g.name, g.alert_on_entry
FROM geofences g
WHERE ST_Contains(g.area, $user_location)
  AND g.alert_on_entry = true;
```

---

## Quick Reference Commands

```sql
-- Enable PostGIS
CREATE EXTENSION postgis;

-- Create geometry column
ALTER TABLE mytable ADD COLUMN geom GEOMETRY(Point, 4326);

-- Create spatial index
CREATE INDEX idx_mytable_geom ON mytable USING GIST(geom);

-- Update geometry from lat/lon
UPDATE mytable 
SET geom = ST_SetSRID(ST_MakePoint(lon, lat), 4326);

-- Distance query (meters)
SELECT ST_Distance(geom1::geography, geom2::geography);

-- Within radius query
SELECT * FROM mytable
WHERE ST_DWithin(geom::geography, point::geography, radius_meters);

-- Export as GeoJSON
SELECT ST_AsGeoJSON(geom);

-- Export as WKT
SELECT ST_AsText(geom);
```

---

## Resources

- **Official Documentation**: https://postgis.net/documentation/
- **PostGIS in Action**: Book by Regina Obe and Leo Hsu
- **Spatial SQL**: Practice queries at https://postgis.net/workshops/
- **QGIS**: Desktop GIS tool for visualizing PostGIS data

---

## Practice Exercises

1. Create a table of cities with population and location
2. Find all cities within 100km of a given point
3. Calculate the total area of all parks in a city
4. Find the shortest route between two points using road network
5. Create a heatmap of customer locations
6. Implement a "find my nearest" feature
7. Build a geofencing alert system

Happy mapping! 🗺️
