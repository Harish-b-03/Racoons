"""
User Location API Example
FastAPI endpoints for tracking user locations and finding nearby users
"""

from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel, Field, validator
from typing import List, Optional
from datetime import datetime
import psycopg2
from psycopg2.extras import RealDictCursor
from contextlib import contextmanager

app = FastAPI(title="User Location API")

# Database configuration
DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "database": "spatial_db",
    "user": "admin",
    "password": "secretpassword"
}


# Pydantic Models
class LocationUpdate(BaseModel):
    user_id: int = Field(..., gt=0, description="User ID")
    longitude: float = Field(..., ge=-180, le=180, description="Longitude (-180 to 180)")
    latitude: float = Field(..., ge=-90, le=90, description="Latitude (-90 to 90)")
    accuracy: Optional[float] = Field(None, ge=0, description="GPS accuracy in meters")


class NearbyUsersRequest(BaseModel):
    user_id: int = Field(..., gt=0, description="User ID to search from")
    radius_meters: float = Field(1000, gt=0, le=50000, description="Search radius in meters (max 50km)")
    max_age_minutes: int = Field(30, gt=0, le=1440, description="Max age of location data in minutes")


class PointSearchRequest(BaseModel):
    longitude: float = Field(..., ge=-180, le=180)
    latitude: float = Field(..., ge=-90, le=90)
    radius_meters: float = Field(1000, gt=0, le=50000)
    max_age_minutes: int = Field(30, gt=0, le=1440)


class UserLocationResponse(BaseModel):
    user_id: int
    username: str
    email: str
    distance_meters: float
    latitude: float
    longitude: float
    last_updated: datetime


class LocationUpdateResponse(BaseModel):
    success: bool
    message: str
    user_id: int
    latitude: float
    longitude: float


# Database connection
@contextmanager
def get_db_connection():
    conn = psycopg2.connect(**DB_CONFIG)
    try:
        yield conn
        conn.commit()
    except Exception as e:
        conn.rollback()
        raise e
    finally:
        conn.close()


# API Endpoints

@app.post("/api/location/update", response_model=LocationUpdateResponse)
async def update_location(location: LocationUpdate):
    """
    Update or insert a user's current location
    
    Example:
    ```json
    {
        "user_id": 1,
        "longitude": -73.9855,
        "latitude": 40.7580,
        "accuracy": 10.0
    }
    ```
    """
    try:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    """
                    SELECT * FROM update_user_location(%s, %s, %s, %s)
                    """,
                    (location.user_id, location.longitude, location.latitude, location.accuracy)
                )
                result = cur.fetchone()
                
                if not result['success']:
                    raise HTTPException(status_code=400, detail=result['message'])
                
                return LocationUpdateResponse(
                    success=True,
                    message=result['message'],
                    user_id=location.user_id,
                    latitude=location.latitude,
                    longitude=location.longitude
                )
    except psycopg2.Error as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")


@app.post("/api/users/nearby", response_model=List[UserLocationResponse])
async def find_nearby_users(request: NearbyUsersRequest):
    """
    Find all users within a specified radius of a given user
    
    Example:
    ```json
    {
        "user_id": 1,
        "radius_meters": 1000,
        "max_age_minutes": 30
    }
    ```
    """
    try:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    """
                    SELECT * FROM find_nearby_users(%s, %s, %s)
                    """,
                    (request.user_id, request.radius_meters, request.max_age_minutes)
                )
                results = cur.fetchall()
                
                return [UserLocationResponse(**row) for row in results]
    except psycopg2.Error as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")


@app.post("/api/users/near-point", response_model=List[UserLocationResponse])
async def find_users_near_point(request: PointSearchRequest):
    """
    Find all users within a specified radius of a coordinate point
    
    Example:
    ```json
    {
        "longitude": -73.9855,
        "latitude": 40.7580,
        "radius_meters": 1000,
        "max_age_minutes": 30
    }
    ```
    """
    try:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    """
                    SELECT * FROM find_users_near_point(%s, %s, %s, %s)
                    """,
                    (request.longitude, request.latitude, request.radius_meters, request.max_age_minutes)
                )
                results = cur.fetchall()
                
                return [UserLocationResponse(**row) for row in results]
    except psycopg2.Error as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")


@app.get("/api/location/{user_id}")
async def get_user_location(user_id: int):
    """
    Get the current location of a specific user
    """
    try:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    """
                    SELECT 
                        u.id as user_id,
                        u.username,
                        ST_Y(ul.location) as latitude,
                        ST_X(ul.location) as longitude,
                        ul.accuracy,
                        ul.updated_at,
                        EXTRACT(EPOCH FROM (NOW() - ul.updated_at))/60 as minutes_ago
                    FROM users u
                    LEFT JOIN user_locations ul ON u.id = ul.user_id
                    WHERE u.id = %s
                    """,
                    (user_id,)
                )
                result = cur.fetchone()
                
                if not result:
                    raise HTTPException(status_code=404, detail="User not found")
                
                if result['latitude'] is None:
                    raise HTTPException(status_code=404, detail="User location not available")
                
                return result
    except psycopg2.Error as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")


@app.get("/api/users/active")
async def get_active_users(max_age_minutes: int = 30):
    """
    Get all users with recent location updates
    """
    try:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    """
                    SELECT 
                        u.id as user_id,
                        u.username,
                        ST_Y(ul.location) as latitude,
                        ST_X(ul.location) as longitude,
                        ul.accuracy,
                        ul.updated_at,
                        EXTRACT(EPOCH FROM (NOW() - ul.updated_at))/60 as minutes_ago
                    FROM users u
                    JOIN user_locations ul ON u.id = ul.user_id
                    WHERE ul.updated_at > NOW() - (%s || ' minutes')::INTERVAL
                    ORDER BY ul.updated_at DESC
                    """,
                    (max_age_minutes,)
                )
                results = cur.fetchall()
                
                return results
    except psycopg2.Error as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")


@app.delete("/api/location/{user_id}")
async def delete_user_location(user_id: int):
    """
    Delete a user's location data
    """
    try:
        with get_db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "DELETE FROM user_locations WHERE user_id = %s RETURNING user_id",
                    (user_id,)
                )
                result = cur.fetchone()
                
                if not result:
                    raise HTTPException(status_code=404, detail="User location not found")
                
                return {"success": True, "message": "Location deleted successfully"}
    except psycopg2.Error as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
