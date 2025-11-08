# Bookverse Gateway Service

A robust and secure API Gateway service built with Spring Cloud Gateway for the Bookverse microservices ecosystem. This service acts as a single entry point for all downstream microservices, providing centralized authentication, authorization, and routing capabilities.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Gateway Routes](#api-gateway-routes)
- [Authentication & Authorization](#authentication--authorization)
- [Public Endpoints](#public-endpoints)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Running the Application](#running-the-application)
- [Environment Variables](#environment-variables)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🎯 Overview

The Bookverse Gateway Service is a microservices API gateway that provides:

- **Centralized Routing**: Single entry point for all microservices (Book Service, Review Service, User Service)
- **JWT-based Authentication**: Secure token validation for protected endpoints
- **Service Discovery**: Integration with Netflix Eureka for dynamic service discovery
- **Load Balancing**: Automatic load balancing across service instances
- **Global Error Handling**: Consistent error responses across all services
- **Reactive Architecture**: Built on Spring WebFlux for high-performance, non-blocking operations

## 🏗️ Architecture

```
Client Request
      ↓
API Gateway (Port 8080)
      ↓
[Authentication Filter]
      ↓
[Route Resolution]
      ↓
[Service Discovery via Eureka]
      ↓
Downstream Services:
  - Book Service
  - Review Service
  - User Service
```

## ✨ Features

### 1. **JWT Authentication**
- Token validation for all protected endpoints
- Automatic extraction of user information (username, roles)
- Custom headers (`X-User-ID`, `X-User-Roles`) passed to downstream services

### 2. **Flexible Public Endpoint Configuration**
- HTTP method-specific public endpoints
- Ant-style path pattern matching
- YAML-based configuration for easy maintenance

### 3. **Service Discovery Integration**
- Automatic service registration and discovery via Netflix Eureka
- Dynamic routing based on service names
- Load balancing across multiple instances

### 4. **Global Error Handling**
- Centralized exception handling
- Custom error responses with consistent structure
- Connection error detection and user-friendly messages

### 5. **Security**
- Spring Security integration
- CSRF protection disabled (stateless API)
- Stateless session management

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming Language |
| Spring Boot | 3.5.7 | Application Framework |
| Spring Cloud | 2025.0.0 | Cloud-native Patterns |
| Spring Cloud Gateway | - | API Gateway Implementation |
| Spring WebFlux | - | Reactive Web Framework |
| Spring Security | - | Security Framework |
| Netflix Eureka Client | - | Service Discovery |
| JJWT | 0.13.0 | JWT Token Processing |
| Lombok | - | Boilerplate Code Reduction |
| Maven | - | Build Tool |

## 📋 Prerequisites

Before running this service, ensure you have:

- **Java Development Kit (JDK) 17** or higher
- **Maven 3.6+** for building the project
- **Netflix Eureka Server** running on `http://localhost:8761`
- **Downstream Services**: Book Service, Review Service, and User Service (optional for testing)

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd bookverse-gateway
```

### 2. Build the Project

```bash
./mvnw clean install
```

Or on Windows:

```bash
mvnw.cmd clean install
```

### 3. Run Tests

```bash
./mvnw test
```

## ⚙️ Configuration

### Application Properties (`application.properties`)

```properties
# Application Name
spring.application.name=bookverse-gateway

# Server Port
server.port=8080

# Eureka Client Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# JWT Secret Key
jwt.secret=<your-secret-key>

# Gateway Discovery
spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true
```

### Public Endpoints Configuration (`application.yaml`)

```yaml
bookverse-security:
  public-endpoints:
    GET:
      - /api/books/**
      - /api/book/**
      - /api/review/**
      - /api/books
    POST:
      - /api/auth/**
```

## 🛣️ API Gateway Routes

The gateway automatically routes requests to downstream services:

### Route Configuration

| Route ID | Service Name | Path Pattern | Description |
|----------|--------------|--------------|-------------|
| book-service-route | book-service | /book-service/** | Routes to Book Service |
| review-service-route | review-service | /review-service/** | Routes to Review Service |
| user-service-route | user-service | /user-service/** | Routes to User Service |

### Example Requests

#### Public Endpoint (No Authentication Required)
```bash
GET http://localhost:8080/api/books
```

#### Protected Endpoint (Authentication Required)
```bash
POST http://localhost:8080/api/books
Authorization: Bearer <jwt-token>
```

## 🔐 Authentication & Authorization

### How It Works

1. **Client sends request** with JWT token in Authorization header:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

2. **AuthenticationFilter validates** the token:
   - Checks if endpoint is public (skip authentication)
   - Validates token signature and expiration
   - Extracts user information (username, roles)

3. **Gateway adds custom headers** for downstream services:
   ```
   X-User-ID: john.doe
   X-User-Roles: USER,ADMIN
   ```

4. **Request is forwarded** to the appropriate downstream service

### JWT Token Structure

The gateway expects JWT tokens with the following claims:

```json
{
  "sub": "username",
  "roles": ["USER", "ADMIN"],
  "exp": 1699564800,
  "iat": 1699478400
}
```

### Protected vs Public Endpoints

- **Protected Endpoints**: Require valid JWT token in Authorization header
- **Public Endpoints**: Configured in `application.yaml`, accessible without authentication

## 🌐 Public Endpoints

Public endpoints are configured per HTTP method in `application.yaml`:

### Configuration Format

```yaml
bookverse-security:
  public-endpoints:
    <HTTP_METHOD>:
      - <ant-path-pattern>
```

### Current Public Endpoints

| Method | Pattern | Description |
|--------|---------|-------------|
| GET | /api/books/** | List all books or get book details |
| GET | /api/book/** | Alternative book endpoints |
| GET | /api/review/** | View reviews |
| POST | /api/auth/** | Authentication endpoints (login, register) |

### Path Pattern Matching

The gateway uses **Ant-style path patterns**:
- `*` - matches any single path segment
- `**` - matches zero or more path segments
- `?` - matches a single character

**Examples:**
- `/api/books/**` matches `/api/books`, `/api/books/123`, `/api/books/123/details`
- `/api/auth/*` matches `/api/auth/login`, `/api/auth/register` (but not `/api/auth/user/profile`)

## 🚨 Error Handling

### Global Error Handler

The `GlobalErrorWebExceptionHandler` provides centralized error handling:

#### Error Response Format

```json
{
  "timestamp": "2025-11-09T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid Authorization Token",
  "path": "/api/books"
}
```

#### Common Error Scenarios

| Scenario | Status Code | Message |
|----------|-------------|---------|
| Missing Authorization Header | 401 | "Missing Authorization Header" |
| Malformed Token | 401 | "Missing or Malformed Authorization Header" |
| Invalid/Expired Token | 401 | "Invalid Authorization Token" |
| Service Unavailable | 503 | "Gateway could not connect to the downstream service" |
| Connection Refused | 503 | "Gateway could not connect to the downstream service" |

## 📁 Project Structure

```
bookverse-gateway/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/bookverse/bookverse_gateway/
│   │   │       ├── AuthenticationFilter.java          # Global JWT authentication filter
│   │   │       ├── BookverseGatewayApplication.java  # Main application class
│   │   │       ├── config/
│   │   │       │   ├── PublicEndpointConfig.java     # Public endpoint configuration
│   │   │       │   └── SecurityConfig.java            # Spring Security configuration
│   │   │       ├── exception/
│   │   │       │   ├── GlobalErrorWebExceptionHandler.java  # Global error handler
│   │   │       │   └── JwtAuthenticationException.java      # Custom JWT exception
│   │   │       └── util/
│   │   │           └── JwtUtil.java                   # JWT utility methods
│   │   └── resources/
│   │       ├── application.properties                 # Main configuration
│   │       └── application.yaml                       # Public endpoints config
│   └── test/
│       └── java/
│           └── com/bookverse/bookverse_gateway/
│               ├── exception/
│               │   └── GlobalErrorWebExceptionHandlerTest.java
│               ├── filter/
│               │   └── AuthenticationFilterTest.java
│               └── util/
│                   └── JwtUtilTest.java
├── pom.xml                                            # Maven dependencies
└── README.md                                          # This file
```

### Key Components

#### `AuthenticationFilter.java`
- Implements `GlobalFilter` for all gateway requests
- Checks if endpoint is public or requires authentication
- Validates JWT tokens
- Adds user information to request headers

#### `JwtUtil.java`
- Parses and validates JWT tokens
- Extracts claims (username, roles, expiration)
- Uses HMAC-SHA256 signature verification

#### `PublicEndpointConfig.java`
- Loads public endpoint configuration from YAML
- Uses `@ConfigurationProperties` for type-safe configuration
- Provides method-specific endpoint lists

#### `SecurityConfig.java`
- Configures Spring Security for reactive web
- Disables form login and HTTP basic auth
- Enables stateless session management

#### `GlobalErrorWebExceptionHandler.java`
- Handles all exceptions globally
- Provides consistent error response format
- Detects connection errors and provides user-friendly messages

## 🧪 Testing

The project includes comprehensive unit tests:

### Run All Tests

```bash
./mvnw test
```

### Test Coverage

- **JwtUtilTest**: JWT token validation, parsing, and claims extraction
- **AuthenticationFilterTest**: Filter logic, public endpoint detection, token validation
- **GlobalErrorWebExceptionHandlerTest**: Error handling, response formatting

### Manual Testing with cURL

#### Test Public Endpoint
```bash
curl -X GET http://localhost:8080/api/books
```

#### Test Protected Endpoint (without token)
```bash
curl -X POST http://localhost:8080/api/books
# Expected: 401 Unauthorized
```

#### Test Protected Endpoint (with token)
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"New Book","author":"Author Name"}'
```

## 🏃 Running the Application

### Option 1: Using Maven

```bash
./mvnw spring-boot:run
```

### Option 2: Using Java

```bash
./mvnw clean package
java -jar target/bookverse-gateway-0.0.1-SNAPSHOT.jar
```

### Option 3: Using IDE

1. Open the project in your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Run `BookverseGatewayApplication.java` as a Java application

### Verify the Application is Running

```bash
curl http://localhost:8080/actuator/health
```

Or check the Eureka dashboard at `http://localhost:8761` to see if the gateway is registered.

## 🌍 Environment Variables

You can override configuration using environment variables:

```bash
# Server Port
export SERVER_PORT=8080

# Eureka Server URL
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/

# JWT Secret
export JWT_SECRET=your-secret-key-here

# Run the application
java -jar target/bookverse-gateway-0.0.1-SNAPSHOT.jar
```

### Docker Environment Variables

```dockerfile
ENV SERVER_PORT=8080
ENV EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
ENV JWT_SECRET=your-secret-key-here
```

## 🔧 Troubleshooting

### Common Issues and Solutions

#### 1. **Gateway Cannot Connect to Eureka**

**Problem**: `Unable to connect to Eureka server`

**Solution**: 
- Ensure Eureka server is running on `http://localhost:8761`
- Check network connectivity
- Verify `eureka.client.service-url.defaultZone` in configuration

#### 2. **JWT Token Validation Fails**

**Problem**: `Invalid Authorization Token`

**Solution**:
- Verify JWT secret matches the one used to sign tokens
- Check token expiration time
- Ensure token format: `Bearer <token>`

#### 3. **Service Unavailable (503)**

**Problem**: `Gateway could not connect to the downstream service`

**Solution**:
- Ensure downstream services are running
- Check if services are registered with Eureka
- Verify route configuration in `application.properties`

#### 4. **Public Endpoints Not Working**

**Problem**: Public endpoint returns 401 Unauthorized

**Solution**:
- Check `application.yaml` for correct endpoint patterns
- Verify HTTP method matches configuration
- Enable debug logging: `logging.level.com.bookverse.bookverse_gateway=DEBUG`

#### 5. **Port Already in Use**

**Problem**: `Port 8080 is already in use`

**Solution**:
```bash
# Change port in application.properties
server.port=8081

# Or use environment variable
export SERVER_PORT=8081
```

### Enable Debug Logging

Add to `application.properties`:

```properties
logging.level.com.bookverse.bookverse_gateway=DEBUG
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.org.springframework.security=DEBUG
```

## 📊 Monitoring & Actuator

Spring Boot Actuator endpoints (if enabled):

- `/actuator/health` - Health check
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/gateway/routes` - Configured routes

## 🔒 Security Considerations

1. **JWT Secret**: Store securely, never commit to version control
2. **HTTPS**: Use HTTPS in production environments
3. **Token Expiration**: Implement reasonable token expiration times
4. **Rate Limiting**: Consider adding rate limiting for production
5. **CORS Configuration**: Configure CORS policies as needed

## 📝 API Documentation

### Request Headers

#### Required for Protected Endpoints
- `Authorization: Bearer <jwt-token>` - JWT authentication token

#### Automatically Added by Gateway (for downstream services)
- `X-User-ID: <username>` - Extracted from JWT token
- `X-User-Roles: <role1,role2>` - Comma-separated roles from JWT token

### Response Headers

Standard headers:
- `Content-Type: application/json`
- `X-Response-Time: <milliseconds>` (if configured)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is part of the Bookverse microservices ecosystem.

## 👥 Authors

- Developed as a Gateway Service for Bookverse microservices architecture

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Check the [Troubleshooting](#troubleshooting) section
- Review application logs with debug logging enabled

---

**Last Updated**: November 2025
**Version**: 0.0.1-SNAPSHOT
**Spring Boot**: 3.5.7
**Spring Cloud**: 2025.0.0

