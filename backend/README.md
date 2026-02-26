# Fitness Tracker API

A professional Spring Boot fitness tracking application with real-time GPS tracking via WebSocket, PostGIS spatial queries, and RESTful APIs.

## 🚀 Features

- **Real-time GPS Tracking** - WebSocket-based live location streaming
- **Spatial Queries** - Find nearby users within specified radius using PostGIS
- **JWT Authentication** - Secure authentication with access and refresh tokens
- **Activity Tracking** - Track workouts with GPS routes
- **Redis Caching** - Fast location lookups with Redis cache
- **Clean Architecture** - Layered architecture with clear separation of concerns
- **Docker Support** - Complete containerized deployment

## 📋 Tech Stack

- **Java 17**
- **Spring Boot 3.2.2**
- **PostgreSQL + PostGIS** - Spatial database
- **Redis** - Caching layer
- **WebSocket** - Real-time communication
- **JWT** - Authentication
- **Flyway** - Database migrations
- **Maven** - Build tool
- **Docker** - Containerization

## 🏗️ Project Structure

```
fitness-tracker/
├── src/main/java/com/fitnesstracker/
│   ├── config/              # Configuration classes
│   │   ├── AppConfig.java
│   │   ├── WebSecurityConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── CorsConfig.java
│   ├── controller/          # REST controllers
│   │   ├── AuthController.java
│   │   ├── LocationController.java
│   │   └── HealthController.java
│   ├── dto/                 # Data Transfer Objects
│   ├── entity/              # JPA entities
│   │   ├── User.java
│   │   ├── UserLocation.java
│   │   ├── Activity.java
│   │   └── GpsTrack.java
│   ├── repository/          # JPA repositories
│   ├── service/             # Business logic
│   │   ├── AuthService.java
│   │   ├── LocationService.java
│   │   └── UserDetailsServiceImpl.java
│   ├── security/            # Security components
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtAuthenticationEntryPoint.java
│   ├── websocket/           # WebSocket handlers
│   │   ├── GpsWebSocketHandler.java
│   │   ├── WebSocketSessionManager.java
│   │   └── WebSocketHandshakeInterceptor.java
│   └── exception/           # Exception handling
├── src/main/resources/
│   ├── application.yml      # Application configuration
│   └── db/migration/        # Flyway migrations
├── docker-compose.yml       # Docker services
├── Dockerfile              # Application container
└── pom.xml                 # Maven dependencies
```

## 🚦 Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### 1. Clone and Setup

```bash
cd fitness-tracker

# Copy environment file
cp .env.example .env

# Edit .env with your configuration
nano .env
```

### 2. Run with Docker Compose

```bash
# Start all services (PostgreSQL, Redis, Application)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

The API will be available at `http://localhost:8080/api`

### 3. Run Locally (Development)

```bash
# Start PostgreSQL and Redis
docker-compose up -d postgis redis

# Run application
mvn spring-boot:run

# Or build and run
mvn clean package
java -jar target/fitness-tracker-api-1.0.0.jar
```

## 📡 API Endpoints

### Authentication

#### Register
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com"
    }
  }
}
```

### Location Management

#### Update Location
```http
POST /api/v1/locations/update
Authorization: Bearer {token}
Content-Type: application/json

{
  "latitude": 40.7580,
  "longitude": -73.9855,
  "accuracy": 10.0,
  "altitude": 50.0,
  "speed": 5.5,
  "heading": 180.0
}
```

#### Get My Location
```http
GET /api/v1/locations/me
Authorization: Bearer {token}
```

#### Find Nearby Users (Around Me)
```http
POST /api/v1/locations/nearby/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "radiusMeters": 1000,
  "maxAgeMinutes": 30,
  "limit": 50
}
```

#### Find Nearby Users (Around Coordinates)
```http
POST /api/v1/locations/nearby
Authorization: Bearer {token}
Content-Type: application/json

{
  "latitude": 40.7580,
  "longitude": -73.9855,
  "radiusMeters": 1000,
  "maxAgeMinutes": 30,
  "limit": 50
}
```

#### Get Active Locations
```http
GET /api/v1/locations/active?maxAgeMinutes=30
Authorization: Bearer {token}
```

### Health Check
```http
GET /api/v1/health
```

## 🔌 WebSocket Connection

### Connect to GPS WebSocket

```javascript
// JavaScript/TypeScript example
const token = 'your-jwt-token';
const ws = new WebSocket(`ws://localhost:8080/api/ws/gps?token=${token}`);

ws.onopen = () => {
  console.log('Connected to GPS tracking');
  
  // Send GPS data
  ws.send(JSON.stringify({
    type: 'GPS_DATA',
    data: {
      latitude: 40.7580,
      longitude: -73.9855,
      accuracy: 10.0,
      speed: 5.5,
      heading: 180.0,
      timestamp: new Date().toISOString()
    }
  }));
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};

// Heartbeat
setInterval(() => {
  ws.send(JSON.stringify({ type: 'PING' }));
}, 30000);
```

### WebSocket Message Types

#### Send GPS Data
```json
{
  "type": "GPS_DATA",
  "data": {
    "latitude": 40.7580,
    "longitude": -73.9855,
    "accuracy": 10.0,
    "altitude": 50.0,
    "speed": 5.5,
    "heading": 180.0
  }
}
```

#### Heartbeat
```json
{
  "type": "PING"
}
```

#### Response Messages
```json
{
  "type": "GPS_ACK",
  "data": "Location updated successfully"
}

{
  "type": "PONG",
  "data": "Heartbeat acknowledged"
}

{
  "type": "ERROR",
  "data": "Error message"
}
```

## 🗄️ Database Schema

### Users Table
- User authentication and profile information
- Supports roles (USER, ADMIN)

### User Locations Table
- Current location for each user
- PostGIS Point geometry (SRID 4326)
- Includes accuracy, altitude, speed, heading
- Spatial index for fast proximity queries

### Activities Table
- Workout/activity tracking
- Duration, distance, calories, heart rate
- Status tracking (IN_PROGRESS, COMPLETED, etc.)

### GPS Tracks Table
- Detailed GPS route for each activity
- Point-by-point location history
- Timestamp for each point

## ⚙️ Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fitness_tracker
DB_USER=postgres
DB_PASSWORD=secretpassword

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-256-bit-secret
JWT_EXPIRATION=86400000

# Location
MAX_LOCATION_AGE=30
DEFAULT_RADIUS=1000
MAX_RADIUS=50000

# WebSocket
WS_HEARTBEAT_INTERVAL=30
WS_MAX_CONNECTIONS=3
```

### Application Properties

See `src/main/resources/application.yml` for full configuration options.

## 🧪 Testing

```bash
# Run tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## 📊 Performance

### Spatial Query Complexity
- **Location Update**: O(log N) - B-tree + GIST index
- **Nearby Users Query**: O(M log N + M log M)
  - M = users within radius (typically 10-100)
  - N = total users in database

### Expected Performance
- Location updates: < 10ms
- Nearby queries (1km radius): 10-50ms
- WebSocket latency: < 5ms

## 🔐 Security

- JWT-based authentication
- Password encryption with BCrypt
- CORS configuration
- WebSocket authentication via JWT
- SQL injection prevention (JPA/Hibernate)
- Input validation

## 🚀 Deployment

### Production Checklist

1. **Update JWT Secret**
   ```bash
   # Generate secure secret (256+ bits)
   openssl rand -base64 64
   ```

2. **Configure Database**
   - Use managed PostgreSQL with PostGIS
   - Enable SSL connections
   - Set up backups

3. **Configure Redis**
   - Use managed Redis or Redis Cluster
   - Enable persistence (AOF/RDB)
   - Set password

4. **Environment Variables**
   - Set all production values
   - Never commit secrets to git

5. **Build Production Image**
   ```bash
   docker build -t fitness-tracker:latest .
   docker push your-registry/fitness-tracker:latest
   ```

## 📝 API Documentation

Once running, access Swagger documentation at:
```
http://localhost:8080/api/swagger-ui.html
```

## 🐛 Troubleshooting

### Database Connection Issues
```bash
# Check PostgreSQL is running
docker-compose ps

# View logs
docker-compose logs postgis

# Test connection
docker exec -it fitness_tracker_db psql -U postgres -d fitness_tracker
```

### WebSocket Connection Issues
- Ensure JWT token is valid
- Check CORS/allowed origins configuration
- Verify WebSocket URL format

### Location Queries Not Working
```sql
-- Verify PostGIS extension
SELECT PostGIS_Version();

-- Check spatial index
\d user_locations

-- Test spatial query
SELECT COUNT(*) FROM user_locations 
WHERE ST_DWithin(
  location::geography,
  ST_SetSRID(ST_MakePoint(-73.9855, 40.7580), 4326)::geography,
  1000
);
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostGIS Documentation](https://postgis.net/documentation/)
- [WebSocket Protocol](https://datatracker.ietf.org/doc/html/rfc6455)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

- Your Name - Initial work

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- PostGIS for spatial database capabilities
- Redis for high-performance caching
