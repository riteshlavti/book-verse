# BookVerse User Service

## 📖 Overview

The **User Service** is a core microservice in the BookVerse application ecosystem, responsible for managing user authentication, authorization, and user lifecycle operations. Built with Spring Boot 3.5.7 and Java 17, this service provides secure JWT-based authentication and integrates seamlessly with Netflix Eureka for service discovery.

## 🎯 Purpose

This service handles:
- **User Authentication**: Secure login/logout with JWT token generation
- **User Registration**: New user signup with role-based access control
- **User Management**: CRUD operations for user profiles
- **Authorization**: Role-based access control (RBAC) with ADMIN and USER roles
- **Gateway Integration**: Custom authentication filter for API Gateway integration

## 🏗️ Architecture

### Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.7 | Core framework |
| Java | 17 | Programming language |
| MySQL | 8.x | Database |
| Flyway | 11.16.0 | Database migration |
| JWT (JJWT) | 0.13.0 | Token-based authentication |
| Spring Security | 6.x | Security framework |
| Spring Cloud | 2025.0.0 | Microservices infrastructure |
| Netflix Eureka | Latest | Service discovery |
| Lombok | Latest | Boilerplate reduction |
| Maven | Latest | Build tool |

### Key Components

```
userservice/
├── config/              # Security and filter configurations
├── controller/          # REST API endpoints
├── dto/                # Data Transfer Objects
├── exception/          # Custom exception handlers
├── mapper/             # Entity-DTO mappers
├── model/              # JPA entities
├── repository/         # Database access layer
├── service/            # Business logic layer
└── util/               # Utility classes (JWT)
```

## 🔐 Security Architecture

### Authentication Flow

1. **User Registration** → Password encryption with BCrypt (strength: 12)
2. **User Login** → Credentials validation → JWT token generation
3. **API Request** → Gateway extracts JWT → Forwards headers to service
4. **Service Validation** → GatewayHeaderAuthenticationFilter validates headers
5. **Authorization** → Role-based access control applied

### Security Features

- **Stateless Session Management**: No server-side session storage
- **JWT Token Authentication**: Secure token-based auth with 1-hour expiration
- **BCrypt Password Encoding**: 12-round encryption for password security
- **Role-Based Access Control**: ADMIN and USER roles with different privileges
- **Custom Gateway Filter**: `GatewayHeaderAuthenticationFilter` for header-based authentication
- **CSRF Protection**: Disabled for stateless API (JWT-based)

### Security Configuration

```
Public Endpoints:
  - /api/auth/**          (signup, login, logout)

Protected Endpoints:
  - POST /api/user        (ADMIN only - Create user)
  - DELETE /api/user/**   (ADMIN only - Delete user)
  - PUT /api/user/**      (ADMIN & USER - Update user)
  - GET /api/user/**      (Authenticated - Get user)
```

## 📡 API Endpoints

### Authentication Controller (`/api/auth`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/auth/signup` | Register a new user | Public |
| POST | `/api/auth/login` | Login and get JWT token | Public |
| POST | `/api/auth/logout` | Logout user | Public |
| GET | `/api/auth/me` | Get current user info | Authenticated |

#### Request/Response Examples

**Signup Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "role": "USER"
}
```

**Login Request:**
```json
{
  "username": "johndoe",
  "password": "SecurePass123!"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "johndoe",
  "role": "USER",
  "expiresIn": 3600000
}
```

### User CRUD Controller (`/api/user`)

| Method | Endpoint | Description | Access | Headers Required |
|--------|----------|-------------|--------|------------------|
| POST | `/api/user` | Create a new user | ADMIN | X-User-Id, X-User-Roles |
| GET | `/api/user/{username}` | Get user by username | Authenticated | X-User-Id |
| PUT | `/api/user/{username}` | Update user profile | ADMIN/USER | X-User-Id |
| DELETE | `/api/user/{username}` | Delete user | ADMIN | X-User-Id, X-User-Roles |

**Note**: These endpoints expect headers from the API Gateway containing authenticated user information.

## 💾 Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email_id VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER'),
    created_at DATETIME(6),
    updated_at DATETIME(6)
);
```

### Entity Model

```java
@Entity
@Table(name = "users")
public class User {
    private Long id;
    private String username;
    private String emailId;
    private String password;
    private Role role;
    private Date createdAt;
    private Date updatedAt;
}
```

### Roles

- **ADMIN**: Full access to all operations
- **USER**: Limited access (can update own profile)

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **MySQL 8.x** database server
- **Maven 3.6+** for building
- **Netflix Eureka Server** (for service discovery)

### Database Setup

1. Create MySQL database:
```sql
CREATE DATABASE user_db;
```

2. Update credentials in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/user_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# Application Config
spring.application.name=user-service
server.port=8083

# Database Config
spring.datasource.url=jdbc:mysql://localhost:3306/user_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Config
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Flyway Migration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# JWT Config
jwt.secret=your_secret_key_here
jwt.expiration=3600000  # 1 hour in milliseconds
```

### Build and Run

1. **Clone the repository** (if not already done)

2. **Build the project:**
```bash
cd userservice
mvn clean install
```

3. **Run the application:**
```bash
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/userservice-0.0.1-SNAPSHOT.jar
```

4. **Verify the service is running:**
```bash
curl http://localhost:8083/actuator/health
```

### Service Discovery

The service automatically registers with Eureka Server on startup. Ensure your Eureka Server is running before starting this service.

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthControllerTest

# Run tests with coverage
mvn clean test jacoco:report
```

### Test Coverage

The project includes comprehensive test coverage:
- **Controller Tests**: `AuthControllerTest`, `UserCrudControllerTest`
- **Service Tests**: `AuthServiceTest`, `UserCrudServiceTest`, `CustomUserDetailsServiceTest`

## 📁 Project Structure

```
userservice/
│
├── src/main/java/com/bookverse/userservice/
│   ├── UserserviceApplication.java          # Main application entry point
│   │
│   ├── config/
│   │   ├── SecurityConfig.java              # Spring Security configuration
│   │   └── GatewayHeaderAuthenticationFilter.java  # Custom auth filter
│   │
│   ├── controller/
│   │   ├── AuthController.java              # Authentication endpoints
│   │   └── UserCrudController.java          # User CRUD endpoints
│   │
│   ├── dto/
│   │   ├── auth/                            # Authentication DTOs
│   │   │   ├── LoginRequestDto.java
│   │   │   ├── LoginResponseDto.java
│   │   │   └── SignUpDto.java
│   │   └── user/                            # User DTOs
│   │       ├── UserRequestDto.java
│   │       └── UserResponseDto.java
│   │
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java      # Centralized exception handling
│   │   └── serviceLevel/                    # Custom exceptions
│   │
│   ├── mapper/
│   │   └── UserMapper.java                  # Entity-DTO mapping
│   │
│   ├── model/
│   │   ├── User.java                        # User entity
│   │   └── Role.java                        # Role enum
│   │
│   ├── repository/
│   │   └── UserRepository.java              # JPA repository
│   │
│   ├── service/
│   │   ├── AuthService.java                 # Authentication business logic
│   │   ├── UserCrudService.java             # User CRUD business logic
│   │   └── CustomUserDetailsService.java    # Spring Security user details
│   │
│   └── util/
│       └── JwtUtil.java                     # JWT token operations
│
├── src/main/resources/
│   ├── application.properties               # Application configuration
│   └── db/migration/
│       └── V1_create_users_table.sql       # Flyway migration script
│
├── src/test/java/                          # Test classes
│
├── docs/                                    # Additional documentation
│   ├── 00-Quick-Reference.md
│   ├── 01-SecurityConfig-Explained.md
│   ├── 02-JwtUtil-Explained.md
│   ├── 03-JwtAuthenticationFilter-Explained.md
│   ├── 04-CustomUserDetailsService-Explained.md
│   └── 05-Complete-Authentication-Flow.md
│
├── pom.xml                                  # Maven dependencies
└── README.md                                # This file
```

## 🔧 Key Classes Explained

### 1. SecurityConfig

Configures Spring Security with:
- JWT-based authentication
- Stateless session management
- Role-based authorization rules
- Custom gateway authentication filter

### 2. GatewayHeaderAuthenticationFilter

Custom filter that:
- Extracts user information from gateway headers (`X-User-Id`, `X-User-Roles`)
- Creates Spring Security authentication context
- Enables seamless integration with API Gateway

### 3. AuthService

Handles:
- User registration with password encryption
- Login validation and JWT token generation
- Logout operations
- Current user retrieval

### 4. UserCrudService

Manages:
- User creation (admin only)
- User profile retrieval
- User profile updates (with ownership validation)
- User deletion (admin only)

### 5. JwtUtil

Provides:
- JWT token generation
- Token validation
- Claims extraction (username, roles, expiration)
- Token expiration checking

### 6. CustomUserDetailsService

Implements Spring Security's `UserDetailsService`:
- Loads user from database
- Creates Spring Security UserDetails object
- Used by authentication manager

## 🔄 Integration with API Gateway

This service is designed to work behind an API Gateway that:

1. **Receives client requests** with JWT tokens
2. **Validates JWT tokens** using shared secret
3. **Extracts user information** from token
4. **Forwards requests** with headers:
   - `X-User-Id`: Username from token
   - `X-User-Roles`: User roles from token
5. **Routes to User Service**

The `GatewayHeaderAuthenticationFilter` validates these headers and creates the security context.

## 📊 Service Discovery

The service registers with Netflix Eureka using:
```properties
spring.application.name=user-service
```

Other services can discover and communicate with this service using:
```java
@Autowired
private RestTemplate restTemplate;

String userServiceUrl = "http://user-service/api/user/johndoe";
```

## 🛡️ Error Handling

The service includes comprehensive error handling:

- **400 Bad Request**: Invalid input data
- **401 Unauthorized**: Missing or invalid authentication
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: User not found
- **409 Conflict**: Username already exists
- **500 Internal Server Error**: Server-side errors

All errors return structured JSON responses:
```json
{
  "timestamp": "2025-11-09T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Username already exists",
  "path": "/api/auth/signup"
}
```

## 📚 Additional Documentation

For detailed explanations, refer to the `/docs` directory:

- **Quick Reference**: Common operations and commands
- **SecurityConfig Explained**: Deep dive into security configuration
- **JwtUtil Explained**: JWT token handling details
- **JwtAuthenticationFilter Explained**: Filter chain explanation
- **CustomUserDetailsService Explained**: User details loading
- **Complete Authentication Flow**: End-to-end authentication process

## 🔮 Future Enhancements

Potential improvements:
- [ ] Email verification on signup
- [ ] Password reset functionality
- [ ] Two-factor authentication (2FA)
- [ ] OAuth2 social login integration
- [ ] User profile image upload
- [ ] Account lockout after failed login attempts
- [ ] Audit logging for security events
- [ ] Redis caching for user sessions
- [ ] Rate limiting on authentication endpoints

## 🐛 Troubleshooting

### Common Issues

**1. Database Connection Failed**
```
Solution: Verify MySQL is running and credentials are correct
Check: spring.datasource.url, username, password in application.properties
```

**2. JWT Token Invalid**
```
Solution: Ensure jwt.secret is consistent across all services
Check: Token hasn't expired (default: 1 hour)
```

**3. Flyway Migration Failed**
```
Solution: Check if database already has tables
Run: mvn flyway:clean flyway:migrate (CAUTION: This deletes data)
```

**4. Eureka Registration Failed**
```
Solution: Ensure Eureka server is running
Check: Eureka server URL in application.properties
```

## 📞 Support & Contact

For issues, questions, or contributions:
- Create an issue in the project repository
- Check existing documentation in `/docs`
- Review test cases for usage examples

## 📄 License

[Add your license information here]

---

**Built with ❤️ for BookVerse Ecosystem**

