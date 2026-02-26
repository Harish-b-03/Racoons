# User Location Tracking System

A complete implementation for tracking user locations and finding nearby users within a 1km radius using PostGIS.

## 📁 Files Created

1. **`user_location_setup.sql`** - Database schema, functions, and sample data
2. **`api_example.py`** - FastAPI REST API implementation
3. **`README_USER_LOCATION.md`** - This documentation

## 🚀 Quick Start

### 1. Setup Database

```bash
# Start PostGIS container
docker compose up -d

# Run the SQL setup script
docker exec -i postgis psql -U admin -d spatial_db < user_location_setup.sql
```

### 2. Install Python Dependencies

```bash
pip install fastapi uvicorn psycopg2-binary pydantic
```

### 3. Run the API

```bash
python api_example.py
```

The API will be available at `http://localhost:8000`

API Documentation: `http://localhost:8000/docs`

## 📊 Database Schema

### Tables

#### `users`
- `id` - Primary key
- `username` - Unique username
- `email` - Unique email
- `created_at` - Account creation timestamp
- `updated_at` - Last update timestamp

#### `user_locations`
- `id` - Primary key
- `user_id` - Foreign key to users (unique constraint)
- `location` - PostGIS Point geometry (SRID 4326)
- `accuracy` - GPS accuracy in meters
- `updated_at` - Last location update timestamp

### Indexes

- **Spatial Index (GIST)** on `location` - Critical for fast proximity queries
- **Index** on `user_id` - Fast joins with users table
- **Index** on `updated_at` - Filter stale locations efficiently

## 🔧 SQL Functions

### 1. `update_user_location()`

Update or insert a user's current location.

```sql
SELECT * FROM update_user_location(
    1,              -- user_id
    -73.9855,       -- longitude
    40.7580,        -- latitude
    10.0            -- accuracy (optional)
);
```

**Returns:** `{success: boolean, message: text}`

### 2. `find_nearby_users()`

Find users within a radius of a specific user.

```sql
SELECT * FROM find_nearby_users(
    1,              -- user_id
    1000,           -- radius in meters (1km)
    30              -- max age in minutes (optional, default 30)
);
```

**Returns:**
- `user_id` - User ID
- `username` - Username
- `email` - Email address
- `distance_meters` - Distance from search user
- `latitude` - User's latitude
- `longitude` - User's longitude
- `last_updated` - Last location update time

### 3. `find_users_near_point()`

Find users within a radius of specific coordinates.

```sql
SELECT * FROM find_users_near_point(
    -73.9855,       -- longitude
    40.7580,        -- latitude
    1000,           -- radius in meters
    30              -- max age in minutes (optional)
);
```

**Returns:** Same as `find_nearby_users()`

## 🌐 API Endpoints

### Update Location
```http
POST /api/location/update
Content-Type: application/json

{
    "user_id": 1,
    "longitude": -73.9855,
    "latitude": 40.7580,
    "accuracy": 10.0
}
```

### Find Nearby Users (by User ID)
```http
POST /api/users/nearby
Content-Type: application/json

{
    "user_id": 1,
    "radius_meters": 1000,
    "max_age_minutes": 30
}
```

### Find Users Near Point (by Coordinates)
```http
POST /api/users/near-point
Content-Type: application/json

{
    "longitude": -73.9855,
    "latitude": 40.7580,
    "radius_meters": 1000,
    "max_age_minutes": 30
}
```

### Get User Location
```http
GET /api/location/{user_id}
```

### Get Active Users
```http
GET /api/users/active?max_age_minutes=30
```

### Delete User Location
```http
DELETE /api/location/{user_id}
```

## 💡 Usage Examples

### Example 1: Update Your Location

```python
import requests

response = requests.post('http://localhost:8000/api/location/update', json={
    "user_id": 1,
    "longitude": -73.9855,
    "latitude": 40.7580,
    "accuracy": 10.0
})

print(response.json())
# Output: {"success": true, "message": "Location updated successfully", ...}
```

### Example 2: Find Users Within 1km

```python
response = requests.post('http://localhost:8000/api/users/nearby', json={
    "user_id": 1,
    "radius_meters": 1000,
    "max_age_minutes": 30
})

nearby_users = response.json()
for user in nearby_users:
    print(f"{user['username']}: {user['distance_meters']:.0f}m away")

# Output:
# bob: 500m away
# charlie: 800m away
```

### Example 3: Direct SQL Query

```sql
-- Update my location
SELECT * FROM update_user_location(1, -73.9855, 40.7580, 10.0);

-- Find users within 1km of me
SELECT 
    username,
    ROUND(distance_meters::numeric, 0) as distance_m,
    latitude,
    longitude
FROM find_nearby_users(1, 1000, 30);
```

## 🎯 Key Features

### 1. **Efficient Proximity Search**
- Uses PostGIS `ST_DWithin` with geography type for accurate distance calculations
- GIST spatial index ensures fast queries even with millions of users
- Results ordered by distance (nearest first)

### 2. **Stale Location Filtering**
- Only returns users with recent location updates (default: 30 minutes)
- Prevents showing outdated positions
- Configurable via `max_age_minutes` parameter

### 3. **Accurate Distance Calculation**
- Uses geography type (spherical earth model) for real-world accuracy
- Returns distance in meters
- Supports global coordinates (WGS84 / SRID 4326)

### 4. **Upsert Pattern**
- One user = one current location (enforced by unique constraint)
- Updates existing location or inserts new one
- Automatically updates timestamp

### 5. **GPS Accuracy Tracking**
- Stores GPS accuracy for quality assessment
- Can be used to filter low-quality locations
- Optional field (can be NULL)

## 🔍 Performance Considerations

### Query Performance
```sql
-- GOOD: Uses spatial index
SELECT * FROM find_nearby_users(1, 1000);

-- ALSO GOOD: Direct query with proper indexing
SELECT u.username, ST_Distance(ul.location::geography, point::geography)
FROM user_locations ul
JOIN users u ON ul.user_id = u.id
WHERE ST_DWithin(ul.location::geography, point::geography, 1000);
```

### Index Usage
```sql
-- Check if index is being used
EXPLAIN ANALYZE 
SELECT * FROM find_nearby_users(1, 1000);

-- Look for "Index Scan using idx_user_locations_geom"
```

### Scaling Tips

1. **Partition by region** for global applications
2. **Add caching** for frequently accessed locations
3. **Use connection pooling** (e.g., pgBouncer)
4. **Consider read replicas** for high-traffic scenarios
5. **Implement rate limiting** on location updates

## 🧪 Testing with Sample Data

The SQL script includes 5 sample users around Times Square, NYC:

- **Alice** - At Times Square (center point)
- **Bob** - 500m north (within 1km)
- **Charlie** - 800m east (within 1km)
- **Diana** - 1.5km south (outside 1km)
- **Eve** - 2km west (outside 1km)

Test the queries:
```sql
-- Should return Bob and Charlie
SELECT username, ROUND(distance_meters::numeric, 0) as distance_m
FROM find_nearby_users(
    (SELECT id FROM users WHERE username = 'alice'),
    1000
);
```

## 🔐 Security Considerations

1. **Privacy**
   - Consider implementing privacy zones
   - Allow users to hide their location
   - Implement "ghost mode" feature

2. **Rate Limiting**
   - Limit location update frequency (e.g., max 1/second)
   - Prevent location spam

3. **Data Retention**
   - Consider purging old location data
   - Implement location history table if needed

4. **Authentication**
   - Add JWT/OAuth authentication to API
   - Ensure users can only update their own location
   - Restrict who can see user locations

## 📱 Mobile Integration Example

```javascript
// JavaScript/React Native example
const updateMyLocation = async () => {
    const position = await navigator.geolocation.getCurrentPosition();
    
    await fetch('http://localhost:8000/api/location/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            user_id: currentUserId,
            longitude: position.coords.longitude,
            latitude: position.coords.latitude,
            accuracy: position.coords.accuracy
        })
    });
};

const findNearbyUsers = async () => {
    const response = await fetch('http://localhost:8000/api/users/nearby', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            user_id: currentUserId,
            radius_meters: 1000,
            max_age_minutes: 30
        })
    });
    
    const nearbyUsers = await response.json();
    return nearbyUsers;
};
```

## 🛠️ Troubleshooting

### Issue: No users found
- Check if locations are recent (within `max_age_minutes`)
- Verify coordinates are valid (lat: -90 to 90, lon: -180 to 180)
- Ensure spatial index exists: `\d user_locations`

### Issue: Slow queries
- Verify GIST index: `EXPLAIN ANALYZE SELECT * FROM find_nearby_users(1, 1000);`
- Run `ANALYZE user_locations;` to update statistics
- Check if using geography type (slower but accurate) vs geometry

### Issue: Inaccurate distances
- Ensure using `::geography` cast for real-world distances
- Verify SRID is 4326 (WGS84)
- Check if coordinates are in correct order (longitude, latitude)

## 📚 Next Steps

1. **Add location history** - Track user movement over time
2. **Implement geofencing** - Alert when users enter/exit areas
3. **Add clustering** - Group nearby users on map
4. **Real-time updates** - Use WebSockets for live location sharing
5. **Privacy controls** - Let users control who sees their location
6. **Analytics** - Track popular areas, user density heatmaps

## 🔗 Related Documentation

- PostGIS Crash Course: `POSTGIS_CRASH_COURSE.md`
- Docker Compose: `docker-compose.yml`
- FastAPI Docs: https://fastapi.tiangolo.com/
- PostGIS Reference: https://postgis.net/docs/
