# Bookverse Gateway - Functioning & Architecture Document

## Table of Contents
1. [Overview](#overview)
2. [Gateway Architecture](#gateway-architecture)
3. [Request Processing Flow](#request-processing-flow)
4. [Core Components](#core-components)
5. [Authentication & Authorization Flow](#authentication--authorization-flow)
6. [Service Discovery & Routing](#service-discovery--routing)
7. [Error Handling Mechanism](#error-handling-mechanism)
8. [Configuration Details](#configuration-details)
9. [Security Implementation](#security-implementation)

---

## Overview

The Bookverse Gateway is a **Spring Cloud Gateway** implementation that serves as the single entry point for all client requests in the Bookverse microservices ecosystem. It provides:

- **Centralized Authentication**: JWT-based token validation
- **Intelligent Routing**: Dynamic service discovery and load balancing
- **Security Layer**: Method-specific public endpoint configuration
- **Error Handling**: Global exception handling with user-friendly messages
- **Header Enrichment**: Automatic user context propagation to downstream services

### Technology Foundation
- **Spring Cloud Gateway**: Reactive gateway built on Spring WebFlux
- **Netflix Eureka**: Service discovery and registration
- **JWT (JJWT)**: Token-based authentication
- **Spring Security**: Security framework integration

---

## Gateway Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT REQUEST                          │
│                    (HTTP/HTTPS on port 8080)                    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING CLOUD GATEWAY                         │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │           1. ServerWebExchange Created                     │ │
│  │              (Request + Response wrapper)                  │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              2. AUTHENTICATION FILTER (GlobalFilter)            │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Step 1: Extract Path & HTTP Method                       │ │
│  │  Step 2: Check Public Endpoint Configuration              │ │
│  │  Step 3: If Public → Skip Authentication                  │ │
│  │  Step 4: If Protected → Validate JWT Token                │ │
│  │  Step 5: Extract User Info (username, roles)              │ │
│  │  Step 6: Add Headers (X-User-ID, X-User-Roles)            │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              3. ROUTE PREDICATE EVALUATION                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Match request path with configured routes:                │ │
│  │  • /book-service/**    → book-service                      │ │
│  │  • /review-service/**  → review-service                    │ │
│  │  • /user-service/**    → user-service                      │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              4. SERVICE DISCOVERY (Eureka)                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Query Eureka Server for service instances                │ │
│  │  Select instance (load balancing: lb://)                  │ │
│  │  Resolve actual service URL                               │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              5. APPLY FILTERS (StripPrefix)                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Remove service prefix from path                          │ │
│  │  Example: /book-service/api/books → /api/books            │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              6. FORWARD TO DOWNSTREAM SERVICE                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  HTTP Request with:                                        │ │
│  │  • Modified path                                           │ │
│  │  • Original headers + X-User-ID, X-User-Roles             │ │
│  │  • Request body (if any)                                   │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                  DOWNSTREAM SERVICE RESPONSE                    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         7. GLOBAL ERROR HANDLER (if error occurs)               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Catch exceptions (JwtAuthenticationException, etc.)      │ │
│  │  Format error response with timestamp, status, message    │ │
│  │  Return standardized error JSON                           │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT RESPONSE                            │
│               (JSON with data or error)                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Request Processing Flow

### Flow 1: Public Endpoint Request (e.g., GET /api/books)

```
1. Client Request
   ↓
   GET http://localhost:8080/api/books
   Headers: None (no Authorization)

2. Gateway Receives Request
   ↓
   - ServerWebExchange created
   - Path: /api/books
   - Method: GET

3. AuthenticationFilter.filter()
   ↓
   - Checks: isPublicEndpoint("/api/books", GET)
   - Looks up application.yaml: GET → [/api/books/**]
   - Match found using AntPathMatcher
   - Result: TRUE → Skip authentication

4. Route Matching
   ↓
   - No explicit route matches /api/books
   - Falls through to downstream services
   - Or returns 404 if no service handles it

5. Response to Client
   ↓
   - 200 OK with books data (if service found)
   - OR 404 Not Found
```

### Flow 2: Protected Endpoint Request (e.g., POST /api/books)

```
1. Client Request
   ↓
   POST http://localhost:8080/book-service/api/books
   Headers:
     Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     Content-Type: application/json
   Body:
     {"title": "New Book", "author": "John Doe"}

2. Gateway Receives Request
   ↓
   - ServerWebExchange created
   - Path: /book-service/api/books
   - Method: POST

3. AuthenticationFilter.filter()
   ↓
   Step 1: Check if public endpoint
     - isPublicEndpoint("/book-service/api/books", POST)
     - Checks application.yaml: POST → [/api/auth/**]
     - No match found
     - Result: FALSE → Requires authentication

   Step 2: Extract Authorization header
     - Header present: "Bearer eyJhbG..."
     - Extract token by removing "Bearer " prefix
     - Token: "eyJhbG..."

   Step 3: Validate token using JwtUtil
     - jwtUtil.validateToken(token)
     - Check expiration: extractExpiration(token) > now()
     - Verify signature: Jwts.parser().verifyWith(signKey)
     - Result: Valid

   Step 4: Extract user information
     - Username: jwtUtil.extractUsername(token) → "john.doe"
     - Roles: jwtUtil.extractRoles(token) → ["USER", "ADMIN"]

   Step 5: Mutate request with custom headers
     - Add X-User-ID: john.doe
     - Add X-User-Roles: USER,ADMIN

4. Route Matching
   ↓
   - Path: /book-service/api/books
   - Matches route: spring.cloud.gateway.routes[0]
   - Route ID: book-service-route
   - Predicate: Path=/book-service/**
   - URI: lb://book-service (load balanced)

5. Service Discovery
   ↓
   - Query Eureka: Find instances of "book-service"
   - Available instances: [http://localhost:8081, http://localhost:8082]
   - Load balancer selects: http://localhost:8081

6. Apply Filters
   ↓
   - StripPrefix=1 filter applied
   - Original path: /book-service/api/books
   - Modified path: /api/books

7. Forward to Downstream Service
   ↓
   POST http://localhost:8081/api/books
   Headers:
     Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     X-User-ID: john.doe
     X-User-Roles: USER,ADMIN
     Content-Type: application/json
   Body:
     {"title": "New Book", "author": "John Doe"}

8. Downstream Service Processes Request
   ↓
   - Service reads X-User-ID and X-User-Roles headers
   - Creates book with author info
   - Returns 201 Created

9. Gateway Returns Response to Client
   ↓
   201 Created
   {"id": 123, "title": "New Book", "author": "John Doe"}
```

### Flow 3: Authentication Failure (Invalid Token)

```
1. Client Request
   ↓
   POST http://localhost:8080/book-service/api/books
   Headers:
     Authorization: Bearer invalid_or_expired_token

2. Gateway Receives Request
   ↓
   - Path: /book-service/api/books
   - Method: POST

3. AuthenticationFilter.filter()
   ↓
   Step 1: Check public endpoint → FALSE
   Step 2: Extract token → "invalid_or_expired_token"
   Step 3: Validate token
     - jwtUtil.validateToken(token)
     - Token parsing fails OR expired
     - Result: FALSE
   Step 4: Throw JwtAuthenticationException
     - Exception: "Invalid Authorization Token"

4. GlobalErrorWebExceptionHandler Catches Exception
   ↓
   - Error type: JwtAuthenticationException
   - HTTP Status: 401 Unauthorized
   - Message: "Invalid Authorization Token"

5. Format Error Response
   ↓
   {
     "timestamp": "2025-11-09T10:30:00.000+00:00",
     "status": 401,
     "error": "Unauthorized",
     "message": "Invalid Authorization Token",
     "path": "/book-service/api/books"
   }

6. Return to Client
   ↓
   401 Unauthorized
   (JSON error body)
```

---

## Core Components

### 1. AuthenticationFilter.java

**Purpose**: Global filter that intercepts ALL gateway requests for authentication.

**Key Responsibilities**:
1. Determine if endpoint is public or protected
2. Validate JWT tokens for protected endpoints
3. Extract user information from valid tokens
4. Enrich requests with user context headers

**Code Flow**:
```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 1. Extract request details
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();
    HttpMethod method = request.getMethod();
    
    // 2. Check if public endpoint
    if (isPublicEndpoint(path, method)) {
        return chain.filter(exchange); // Skip authentication
    }
    
    // 3. Validate Authorization header presence
    if (!request.getHeaders().containsKey("Authorization")) {
        throw new JwtAuthenticationException("Missing Authorization Header");
    }
    
    // 4. Extract JWT token
    String authHeader = request.getHeaders().getFirst("Authorization");
    String token = extractToken(authHeader); // Remove "Bearer " prefix
    
    // 5. Validate token
    if (!jwtUtil.validateToken(token)) {
        throw new JwtAuthenticationException("Invalid Authorization Token");
    }
    
    // 6. Extract user info and add headers
    ServerHttpRequest mutatedRequest = request.mutate()
        .header("X-User-ID", jwtUtil.extractUsername(token))
        .header("X-User-Roles", String.join(",", jwtUtil.extractRoles(token)))
        .build();
    
    // 7. Continue filter chain with mutated request
    return chain.filter(exchange.mutate().request(mutatedRequest).build());
}
```

**Public Endpoint Detection**:
```java
private boolean isPublicEndpoint(String path, HttpMethod method) {
    // 1. Get method-specific patterns from config
    List<String> patterns = publicEndpointsConfig
        .getPublicEndpoints()
        .get(method.name()); // "GET", "POST", etc.
    
    // 2. Use AntPathMatcher for pattern matching
    return patterns.stream()
        .anyMatch(pattern -> pathMatcher.match(pattern, path));
}
```

**Pattern Matching Examples**:
- Pattern: `/api/books/**`
  - Matches: `/api/books`, `/api/books/123`, `/api/books/123/reviews`
- Pattern: `/api/auth/*`
  - Matches: `/api/auth/login`, `/api/auth/register`
  - Does NOT match: `/api/auth/user/profile`

---

### 2. JwtUtil.java

**Purpose**: Utility class for JWT token operations.

**Key Methods**:

#### Token Validation
```java
public Boolean validateToken(String token) {
    // 1. Check if token is expired
    if (!isTokenExpired(token)) {
        try {
            // 2. Parse and verify token signature
            Jwts.parser()
                .verifyWith(getSignKey())  // HMAC-SHA256 with secret key
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false; // Invalid signature or malformed token
        }
    }
    return false; // Token expired
}
```

#### Extract Username
```java
public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
    // JWT "sub" claim typically contains username
}
```

#### Extract Roles
```java
public List<String> extractRoles(String token) {
    try {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class); // Custom "roles" claim
    } catch (Exception e) {
        return List.of(); // Return empty list on error
    }
}
```

#### Extract All Claims
```java
public Claims extractAllClaims(String token) {
    JwtParser parser = Jwts.parser()
        .verifyWith(getSignKey())  // Verify signature
        .build();
    Jws<Claims> jws = parser.parseSignedClaims(token);
    return jws.getPayload(); // Return claims
}
```

**JWT Token Structure Expected**:
```json
{
  "sub": "john.doe",           // Username
  "roles": ["USER", "ADMIN"],  // User roles
  "iat": 1699478400,           // Issued at timestamp
  "exp": 1699564800            // Expiration timestamp
}
```

**Secret Key Initialization**:
```java
@PostConstruct
public void init() {
    // Convert string secret to cryptographic key
    signKey = Keys.hmacShaKeyFor(secretKey.getBytes());
}
```

---

### 3. PublicEndpointConfig.java

**Purpose**: Load and manage public endpoint configuration from YAML.

**Configuration Binding**:
```java
@ConfigurationProperties(prefix = "bookverse-security")
public class PublicEndpointConfig {
    private Map<String, List<String>> publicEndpoints;
    // Map structure: {HTTP_METHOD: [path_patterns]}
}
```

**YAML Structure**:
```yaml
bookverse-security:
  public-endpoints:
    GET:
      - /api/books/**
      - /api/review/**
    POST:
      - /api/auth/**
    DELETE:
      - /api/admin/cache/**
```

**In-Memory Representation**:
```
Map {
  "GET" → ["/api/books/**", "/api/review/**"],
  "POST" → ["/api/auth/**"],
  "DELETE" → ["/api/admin/cache/**"]
}
```

---

### 4. GlobalErrorWebExceptionHandler.java

**Purpose**: Centralized error handling for all gateway exceptions.

**Error Response Structure**:
```json
{
  "timestamp": "2025-11-09T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid Authorization Token",
  "path": "/api/books"
}
```

**Error Handling Flow**:
```java
private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    // 1. Get the original error
    Throwable error = getError(request);
    
    // 2. Get default error attributes
    Map<String, Object> errorPropertiesMap = getErrorAttributes(request, ...);
    
    // 3. Determine status code
    int status = errorPropertiesMap.get("status");
    
    // 4. Customize message based on error type
    String errorMessage = errorPropertiesMap.get("message");
    
    // 5. Special handling for connection errors
    if (isConnectionError(error)) {
        errorMessage = "Gateway could not connect to the downstream service";
        status = 503; // Service Unavailable
    }
    
    // 6. Build custom error response
    Map<String, Object> customError = buildErrorMap(status, errorMessage, ...);
    
    // 7. Return JSON response
    return ServerResponse.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(customError);
}
```

**Connection Error Detection**:
```java
private boolean isConnectionError(Throwable error) {
    String errorClass = error.getClass().getName();
    return errorClass.contains("ConnectException") ||
           errorClass.contains("TimeoutException") ||
           error.getMessage().contains("Connection refused");
}
```

**Error Scenarios**:

| Exception Type | Status | Message |
|----------------|--------|---------|
| JwtAuthenticationException | 401 | "Invalid Authorization Token" |
| Missing Authorization | 401 | "Missing Authorization Header" |
| ConnectException | 503 | "Gateway could not connect to service" |
| TimeoutException | 503 | "Gateway could not connect to service" |
| Generic Exception | 500 | Original message or generic fallback |

---

### 5. SecurityConfig.java

**Purpose**: Configure Spring Security for reactive web applications.

**Configuration**:
```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)  // Disable CSRF (stateless API)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)  // No basic auth
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)  // No form login
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        // Stateless: don't store security context
        .authorizeExchange(exchanges -> exchanges
            .anyExchange().permitAll()  // All filtering done by AuthenticationFilter
        )
        .build();
}
```

**Why This Configuration?**
1. **CSRF Disabled**: API gateway is stateless, no session cookies
2. **No HTTP Basic/Form Login**: Using JWT tokens instead
3. **Stateless**: No server-side session storage
4. **Permit All**: AuthenticationFilter handles actual authorization

---

## Authentication & Authorization Flow

### JWT Token Lifecycle

#### 1. Token Generation (by Auth Service)
```
User logs in → Auth Service validates credentials
                ↓
Auth Service generates JWT token:
  {
    "sub": "john.doe",
    "roles": ["USER"],
    "iat": 1699478400,
    "exp": 1699564800
  }
                ↓
Token signed with HMAC-SHA256(secret key)
                ↓
Return to client: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 2. Token Usage (Client → Gateway)
```
Client includes token in request:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                ↓
Gateway receives request → AuthenticationFilter
                ↓
Extract token from header (remove "Bearer " prefix)
                ↓
Validate token:
  - Check signature using same secret key
  - Check expiration date
  - Parse claims
                ↓
If valid: Extract username & roles
          Add to request headers (X-User-ID, X-User-Roles)
          Forward to downstream service
                ↓
If invalid: Throw JwtAuthenticationException
            Return 401 Unauthorized
```

#### 3. Token Validation Details

**Signature Verification**:
```
Token = Header.Payload.Signature

Gateway computes:
  expectedSignature = HMAC-SHA256(Header.Payload, secretKey)

Compare:
  if (token.Signature == expectedSignature) → Valid
  else → Invalid
```

**Expiration Check**:
```
tokenExpiration = token.claims.exp  // Unix timestamp
currentTime = System.currentTimeMillis() / 1000

if (tokenExpiration < currentTime) → Expired
else → Valid
```

---

## Service Discovery & Routing

### Eureka Integration

#### 1. Gateway Registration
```properties
# application.properties
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

**What happens**:
- Gateway registers itself with Eureka on startup
- Gateway periodically fetches service registry (list of all services)
- Gateway receives updates when services start/stop

#### 2. Service Discovery Flow
```
1. Request arrives at gateway: POST /book-service/api/books

2. Route matching finds: lb://book-service
   ("lb" = load balanced)

3. Gateway queries Eureka:
   "Give me all instances of book-service"

4. Eureka returns:
   [
     {host: "localhost", port: 8081, status: "UP"},
     {host: "localhost", port: 8082, status: "UP"}
   ]

5. Load balancer selects instance (round-robin by default):
   Selected: localhost:8081

6. Gateway resolves final URL:
   http://localhost:8081/api/books
   (StripPrefix filter removed /book-service)

7. Gateway forwards request to selected instance
```

### Route Configuration

**Static Routes** (application.properties):
```properties
spring.cloud.gateway.routes[0].id=book-service-route
spring.cloud.gateway.routes[0].uri=lb://book-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/book-service/**
spring.cloud.gateway.routes[0].filters[0]=StripPrefix=1
```

**Route Components**:
1. **ID**: Unique identifier for the route
2. **URI**: Target service (lb:// for load balanced)
3. **Predicates**: Conditions to match (path, method, headers, etc.)
4. **Filters**: Transformations applied to request/response

**StripPrefix Filter**:
```
Original request: /book-service/api/books
StripPrefix=1:    Remove first path segment
Result:           /api/books
```

**Dynamic Discovery** (application.properties):
```properties
spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true
```

**Auto-generated routes**:
- Service name: BOOK-SERVICE
- Auto route: /book-service/** → lb://book-service
- Service name: REVIEW-SERVICE  
- Auto route: /review-service/** → lb://review-service

---

## Error Handling Mechanism

### Exception Hierarchy

```
Throwable
    ↓
Exception
    ↓
RuntimeException
    ↓
JwtAuthenticationException (Custom)
```

### Error Propagation Flow

```
1. Exception occurs in AuthenticationFilter
   ↓
   throw new JwtAuthenticationException("Invalid Authorization Token");

2. Spring WebFlux catches exception
   ↓
   Exception bubbles up through filter chain

3. GlobalErrorWebExceptionHandler intercepts
   ↓
   @Order(-1) ensures it runs before default handlers

4. getRoutingFunction() registers error handler
   ↓
   RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)

5. renderErrorResponse() processes error
   ↓
   - Extract error details
   - Determine HTTP status code
   - Format error message
   - Build JSON response

6. Return ServerResponse to client
   ↓
   {
     "timestamp": "...",
     "status": 401,
     "error": "Unauthorized",
     "message": "Invalid Authorization Token",
     "path": "/api/books"
   }
```

### Error Response Examples

#### Authentication Error
```json
{
  "timestamp": "2025-11-09T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing Authorization Header",
  "path": "/book-service/api/books"
}
```

#### Service Unavailable
```json
{
  "timestamp": "2025-11-09T10:30:00.000+00:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Gateway could not connect to the downstream service. Please try again.",
  "path": "/book-service/api/books"
}
```

#### Internal Server Error
```json
{
  "timestamp": "2025-11-09T10:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/book-service/api/books"
}
```

---

## Configuration Details

### Application Properties Breakdown

```properties
# Service Name
spring.application.name=bookverse-gateway
# → Registers with Eureka as "bookverse-gateway"

# Server Port
server.port=8080
# → Gateway listens on http://localhost:8080

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
# → Location of Eureka server

eureka.client.register-with-eureka=true
# → Register this gateway with Eureka

eureka.client.fetch-registry=true
# → Fetch list of all services from Eureka

# Gateway Discovery
spring.cloud.gateway.discovery.locator.enabled=true
# → Enable automatic route creation from Eureka services

spring.cloud.gateway.discovery.locator.lower-case-service-id=true
# → Convert service names to lowercase in routes
# → BOOK-SERVICE becomes /book-service/**

# JWT Secret
jwt.secret=tpRuunnujyX+kiGsohecL9Ezf0cDp4cU7WurbcgmDTT6ItphCudntlc1Droz6EeP
# → Secret key for JWT signature verification
# → MUST match the secret used by auth service
```

### Route Configuration Explained

**Route 1: Book Service**
```properties
spring.cloud.gateway.routes[0].id=book-service-route
spring.cloud.gateway.routes[0].uri=lb://book-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/book-service/**
spring.cloud.gateway.routes[0].filters[0]=StripPrefix=1
```

**Behavior**:
- Incoming: `POST http://localhost:8080/book-service/api/books`
- Predicate match: `/book-service/**` ✓
- Resolve service: Query Eureka for "book-service" instances
- Select instance: `http://localhost:8081` (example)
- Apply filter: Strip `/book-service` → `/api/books`
- Forward: `POST http://localhost:8081/api/books`

**Route 2: Review Service**
```properties
spring.cloud.gateway.routes[1].id=review-service-route
spring.cloud.gateway.routes[1].uri=lb://review-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/review-service/**
spring.cloud.gateway.routes[1].filters[0]=StripPrefix=1
```

**Route 3: User Service**
```properties
spring.cloud.gateway.routes[2].id=user-service-route
spring.cloud.gateway.routes[2].uri=lb://user-service
spring.cloud.gateway.routes[2].predicates[0]=Path=/user-service/**
spring.cloud.gateway.routes[2].filters[0]=StripPrefix=1
```

### Public Endpoints Configuration

```yaml
bookverse-security:
  public-endpoints:
    GET:
      - /api/books/**     # List/view books
      - /api/book/**      # Alternative book endpoint
      - /api/review/**    # View reviews
      - /api/books        # Exact match for /api/books
    POST:
      - /api/auth/**      # Authentication endpoints (login, register)
```

**How it works**:
1. Configuration loaded into `PublicEndpointConfig` bean
2. Mapped to `Map<String, List<String>>`
3. AuthenticationFilter queries this map
4. Uses AntPathMatcher for flexible pattern matching

---

## Security Implementation

### JWT Secret Key Management

**Secret Key Storage**:
```properties
# application.properties
jwt.secret=tpRuunnujyX+kiGsohecL9Ezf0cDp4cU7WurbcgmDTT6ItphCudntlc1Droz6EeP
```

**Best Practices**:
1. **Never commit secrets to version control**
2. **Use environment variables in production**:
   ```bash
   export JWT_SECRET="your-production-secret"
   ```
3. **Minimum length**: 256 bits (32 bytes) for HS256
4. **Shared secret**: Must match auth service secret

**Key Generation** (for reference):
```java
@PostConstruct
public void init() {
    // Convert string to cryptographic key
    signKey = Keys.hmacShaKeyFor(secretKey.getBytes());
}
```

### HTTPS Consideration

**Current**: HTTP (development)
**Production**: Should use HTTPS

```properties
# Production configuration (example)
server.port=8443
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-store-type=PKCS12
```

### CORS Configuration (if needed)

```java
@Bean
public CorsWebFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("https://bookverse-frontend.com");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    
    UrlBasedCorsConfigurationSource source = 
        new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    
    return new CorsWebFilter(source);
}
```

### Security Headers

Add security headers to responses:
```java
// Add to filter or global filter
response.getHeaders().add("X-Content-Type-Options", "nosniff");
response.getHeaders().add("X-Frame-Options", "DENY");
response.getHeaders().add("X-XSS-Protection", "1; mode=block");
```

---

## Advanced Topics

### Rate Limiting (Future Enhancement)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: book-service-route
          uri: lb://book-service
          predicates:
            - Path=/book-service/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

### Circuit Breaker (Future Enhancement)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: book-service-route
          uri: lb://book-service
          predicates:
            - Path=/book-service/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: bookServiceCircuitBreaker
                fallbackUri: forward:/fallback
```

### Request/Response Logging

```java
@Component
public class LoggingFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Request: {} {}", 
            exchange.getRequest().getMethod(),
            exchange.getRequest().getURI());
        
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Response: {} ({}ms)", 
                exchange.getResponse().getStatusCode(),
                duration);
        }));
    }
}
```

---

## Summary

The Bookverse Gateway provides a robust, secure, and scalable entry point for microservices:

**Key Features**:
✅ JWT-based authentication with flexible public endpoint configuration  
✅ Service discovery and load balancing via Eureka  
✅ Automatic user context propagation to downstream services  
✅ Global error handling with consistent error responses  
✅ Reactive, non-blocking architecture for high performance  
✅ Easy configuration through properties and YAML  

**Request Flow Summary**:
1. Client sends request to gateway
2. AuthenticationFilter checks if public or validates JWT
3. Gateway resolves route and queries Eureka for service instances
4. Load balancer selects instance
5. Request forwarded with user context headers
6. Response returned to client (or error if something fails)

This architecture ensures security, scalability, and maintainability for the Bookverse microservices ecosystem.

---

**Document Version**: 1.0  
**Last Updated**: November 9, 2025  
**Author**: Bookverse Team

