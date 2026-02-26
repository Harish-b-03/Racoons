# Fitness Tracker - Architecture Documentation

## 🏛️ Architecture Overview

This application follows a **Clean Architecture** pattern with clear separation of concerns across layers.

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ REST API     │  │ WebSocket    │  │ Exception    │      │
│  │ Controllers  │  │ Handlers     │  │ Handlers     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Auth         │  │ Location     │  │ Activity     │      │
│  │ Service      │  │ Service      │  │ Service      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ User         │  │ Location     │  │ Activity     │      │
│  │ Repository   │  │ Repository   │  │ Repository   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                                │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │ PostgreSQL   │  │ Redis        │                         │
│  │ + PostGIS    │  │ Cache        │                         │
│  └──────────────┘  └──────────────┘                         │
└─────────────────────────────────────────────────────────────┘
```

## 📦 Layer Responsibilities

### 1. Presentation Layer

**Controllers** (`controller/`)
- Handle HTTP requests/responses
- Input validation
- Route mapping
- Response formatting

**WebSocket Handlers** (`websocket/`)
- Manage WebSocket connections
- Handle real-time GPS data streaming
- Session management
- Message routing

**Exception Handlers** (`exception/`)
- Global exception handling
- Error response formatting
- Logging

### 2. Service Layer

**Business Logic** (`service/`)
- Core application logic
- Transaction management
- Data transformation
- Caching strategies

**Key Services:**
- `AuthService` - User registration, login, JWT generation
- `LocationService` - GPS data processing, spatial queries
- `ActivityService` - Workout tracking, route calculation

### 3. Repository Layer

**Data Access** (`repository/`)
- Database queries
- JPA/Hibernate operations
- Custom spatial queries
- Data persistence

### 4. Security Layer

**Components** (`security/`)
- `JwtService` - Token generation/validation
- `JwtAuthenticationFilter` - Request authentication
- `JwtAuthenticationEntryPoint` - Unauthorized handling
- `UserDetailsServiceImpl` - User loading

### 5. Configuration Layer

**Setup** (`config/`)
- Application configuration
- Security configuration
- WebSocket configuration
- CORS configuration
- Bean definitions

## 🔄 Data Flow

### REST API Request Flow

```
Client Request
    ↓
[JwtAuthenticationFilter] → Validate JWT
    ↓
[Controller] → Validate input, route request
    ↓
[Service] → Business logic, caching
    ↓
[Repository] → Database query
    ↓
[Database] → Data retrieval
    ↓
[Service] → Transform data
    ↓
[Controller] → Format response
    ↓
Client Response
```

### WebSocket Data Flow

```
Client WebSocket Connection
    ↓
[WebSocketHandshakeInterceptor] → Validate JWT
    ↓
[GpsWebSocketHandler] → Handle connection
    ↓
[WebSocketSessionManager] → Manage session
    ↓
Client sends GPS data
    ↓
[GpsWebSocketHandler] → Parse message
    ↓
[LocationService] → Process and save
    ↓
[Repository] → Update database
    ↓
[Cache] → Update Redis cache
    ↓
[GpsWebSocketHandler] → Send acknowledgment
    ↓
Client receives confirmation
```

## 🗄️ Database Design

### Entity Relationships

```
User (1) ──────── (1) UserLocation
  │
  │
  └─── (1:N) ──────── Activity
                        │
                        │
                        └─── (1:N) ──────── GpsTrack
```

### Spatial Indexing Strategy

**GIST Index on user_locations.location**
- Enables fast proximity queries
- O(log N) spatial lookups
- Supports ST_DWithin efficiently

**B-tree Indexes**
- `user_id` for fast user lookups
- `updated_at` for filtering stale data
- `activity_id` for GPS track queries

## 🚀 Performance Optimizations

### 1. Caching Strategy

**Redis Cache Layers:**
```
┌─────────────────────────────────────┐
│  L1: User Location Cache            │
│  TTL: 30 minutes                    │
│  Key: userLocation:{userId}         │
└─────────────────────────────────────┘
         ↓ (on miss)
┌─────────────────────────────────────┐
│  L2: PostgreSQL + PostGIS           │
│  Persistent storage                 │
└─────────────────────────────────────┘
```

**Cache Invalidation:**
- On location update → Evict user cache
- On user deletion → Evict all related caches
- TTL-based expiration for stale data

### 2. Database Optimization

**Connection Pooling (HikariCP):**
- Max pool size: 10
- Min idle: 5
- Connection timeout: 30s

**Query Optimization:**
- Spatial index for location queries
- Composite indexes for common filters
- Batch inserts for GPS tracks

### 3. WebSocket Optimization

**Connection Management:**
- Max 3 connections per user
- Automatic cleanup of stale connections
- Heartbeat mechanism (30s interval)

**Message Processing:**
- Async processing for GPS data
- Batch updates for high-frequency data
- Message queue for reliability

## 🔐 Security Architecture

### Authentication Flow

```
1. User Login
   ↓
2. Validate Credentials
   ↓
3. Generate JWT (Access + Refresh)
   ↓
4. Return Tokens
   ↓
5. Client stores tokens
   ↓
6. Subsequent requests include JWT
   ↓
7. JwtAuthenticationFilter validates
   ↓
8. Set SecurityContext
   ↓
9. Process request
```

### JWT Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "username",
    "userId": 123,
    "iat": 1234567890,
    "exp": 1234571490
  },
  "signature": "..."
}
```

### Security Measures

1. **Password Security**
   - BCrypt hashing (cost factor: 10)
   - Salted passwords
   - No plain text storage

2. **Token Security**
   - HMAC-SHA256 signing
   - Short expiration (1 hour access, 7 days refresh)
   - Secure secret key (256+ bits)

3. **API Security**
   - CORS configuration
   - CSRF protection disabled (stateless JWT)
   - Input validation
   - SQL injection prevention (JPA)

4. **WebSocket Security**
   - JWT authentication on handshake
   - Session validation
   - Origin checking

## 📊 Scalability Considerations

### Horizontal Scaling

**Stateless Design:**
- No server-side sessions
- JWT-based authentication
- Redis for shared state

**Load Balancing:**
```
                    Load Balancer
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
    App Instance 1   App Instance 2   App Instance 3
        │                │                │
        └────────────────┼────────────────┘
                         ↓
                  Shared Resources
                  (PostgreSQL, Redis)
```

**WebSocket Scaling:**
- Sticky sessions for WebSocket connections
- Redis Pub/Sub for cross-instance messaging
- Message broker for reliability (future)

### Database Scaling

**Read Replicas:**
- Master for writes
- Replicas for read queries
- Connection pooling per instance

**Partitioning Strategy:**
- Partition by user_id for large datasets
- Partition GPS tracks by date
- Archive old data

## 🔍 Monitoring & Observability

### Metrics to Track

1. **Application Metrics**
   - Request latency (p50, p95, p99)
   - Throughput (requests/sec)
   - Error rate
   - Active WebSocket connections

2. **Database Metrics**
   - Query execution time
   - Connection pool usage
   - Cache hit ratio
   - Slow query log

3. **Business Metrics**
   - Active users
   - Location updates/sec
   - Average proximity query time
   - GPS data points/day

### Logging Strategy

**Log Levels:**
- ERROR: System errors, exceptions
- WARN: Degraded performance, retries
- INFO: Important events (login, location update)
- DEBUG: Detailed flow (development only)

**Structured Logging:**
```json
{
  "timestamp": "2026-02-21T18:00:00Z",
  "level": "INFO",
  "logger": "LocationService",
  "message": "Location updated",
  "userId": 123,
  "latitude": 40.7580,
  "longitude": -73.9855,
  "traceId": "abc-123"
}
```

## 🧩 Extension Points

### Adding New Features

1. **Activity Types**
   - Add enum value in `Activity.ActivityType`
   - No code changes needed

2. **New Spatial Queries**
   - Add method in `UserLocationRepository`
   - Implement in `LocationService`
   - Expose via `LocationController`

3. **Real-time Notifications**
   - Implement WebSocket broadcast
   - Add Redis Pub/Sub
   - Create notification service

4. **Social Features**
   - Add Friend entity
   - Create FriendService
   - Extend location queries

## 🎯 Design Patterns Used

1. **Repository Pattern** - Data access abstraction
2. **Service Layer Pattern** - Business logic encapsulation
3. **DTO Pattern** - Data transfer objects
4. **Builder Pattern** - Entity construction
5. **Strategy Pattern** - Authentication strategies
6. **Observer Pattern** - WebSocket event handling
7. **Singleton Pattern** - Configuration beans

## 📝 Best Practices Implemented

- ✅ Dependency Injection
- ✅ Interface-based programming
- ✅ Transaction management
- ✅ Exception handling
- ✅ Input validation
- ✅ Logging
- ✅ Configuration externalization
- ✅ Database migrations (Flyway)
- ✅ API versioning
- ✅ Documentation
