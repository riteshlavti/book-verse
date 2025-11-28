# Review Service - BookVerse

A microservice-based review management system for the BookVerse platform, built with Spring Boot. This service handles all operations related to book reviews, including CRUD operations and analytics features like rating calculations using different strategies.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Security](#security)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Design Patterns](#design-patterns)
- [Contributing](#contributing)

## 🔍 Overview

The Review Service is a core component of the BookVerse microservices ecosystem. It manages book reviews, allowing users to create, read, update, and delete reviews for books. The service also provides analytics capabilities to calculate book ratings using different strategies.

This service is designed to be:
- **Scalable**: Built as a microservice that can be independently deployed and scaled
- **Resilient**: Integrates with service discovery (Eureka) and uses Feign for inter-service communication
- **Secure**: Implements custom security filters for authentication via gateway headers
- **Maintainable**: Follows clean architecture principles with clear separation of concerns

## ✨ Features

### Core Functionality
- **Review Management**
  - Create new reviews with rating and text
  - Retrieve reviews by ID or book ID
  - Update existing reviews (with ownership validation)
  - Delete reviews (with ownership validation)

### Analytics
- **Rating Calculation**
  - Multiple rating calculation strategies (Strategy Pattern)
  - Average Rating Strategy
  - Weighted Rating Strategy
  - Configurable strategy selection via API

### Integration
- **Service Communication**
  - Feign client integration with Book Service
  - Book validation before creating reviews
  - Service discovery with Netflix Eureka

### Data Management
- **Database Migration**
  - Flyway integration for version-controlled schema management
  - Automated migration on application startup

## 🛠 Technology Stack

### Core Framework
- **Spring Boot**: 3.5.7
- **Java**: 17

### Spring Modules
- **Spring Data JPA**: Database persistence
- **Spring Data REST**: RESTful API support
- **Spring Web**: Web application framework
- **Spring Validation**: Input validation
- **Spring Security**: Authentication and authorization
- **Spring Cloud Netflix Eureka Client**: Service discovery
- **Spring Cloud OpenFeign**: Declarative REST clients

### Database
- **MySQL**: Primary database
- **Flyway**: Database migration management (v11.16.0)

### Development Tools
- **Lombok**: Reduces boilerplate code
- **Spring DevTools**: Hot reload during development
- **Maven**: Build automation

### Testing
- **Spring Boot Test**: Testing framework
- **Spring Security Test**: Security testing
- **Spring REST Docs**: API documentation from tests

## 🏗 Architecture

### Microservices Pattern
The Review Service follows a microservices architecture:

```
┌─────────────────┐
│   API Gateway   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼───────┐
│ Review│ │  Book    │
│Service│─┤ Service  │
└───┬───┘ └──────────┘
    │
┌───▼───────┐
│   MySQL   │
│ Database  │
└───────────┘
```

### Service Discovery
- Registered with Eureka Server for service discovery
- Other services can locate this service dynamically

### Inter-Service Communication
- Uses Feign Client to communicate with Book Service
- Validates book existence before creating reviews

## 📦 Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- **Eureka Server** (running on default port)
- **API Gateway** (for routing requests)

## 🚀 Installation

### 1. Clone the Repository
```bash
git clone <repository-url>
cd reviewservice
```

### 2. Create Database
```sql
CREATE DATABASE review_db;
```

### 3. Configure Database
Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/review_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. Build the Project
```bash
./mvnw clean install
```

### 5. Run the Application
```bash
./mvnw spring-boot:run
```

The service will start on **port 8082**.

## ⚙️ Configuration

### Application Properties

```properties
# Application Name
spring.application.name=review-service

# Server Port
server.port=8082

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/review_db
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.clean-disabled=true

# Logging
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate=DEBUG
```

## 📡 API Endpoints

### Review Management

#### Get Review by ID
```http
GET /api/review/{id}
```
**Response**: `ReviewResponseDto`

#### Get Reviews by Book ID
```http
GET /api/review/book/{bookId}
```
**Response**: `List<ReviewResponseDto>`

#### Create Review
```http
POST /api/review/
Content-Type: application/json
X-User-Id: {reviewerId}

{
  "bookId": 1,
  "reviewText": "Great book!",
  "rating": 5.0
}
```
**Response**: `ReviewResponseDto`

#### Update Review
```http
PUT /api/review/{id}
Content-Type: application/json
X-User-Id: {reviewerId}

{
  "bookId": 1,
  "reviewText": "Updated review",
  "rating": 4.5
}
```
**Response**: `ReviewResponseDto`

#### Delete Review
```http
DELETE /api/review/{id}
X-User-Id: {reviewerId}
```
**Response**: Success message

### Analytics

#### Get Book Average Rating
```http
GET /api/review/book/{bookId}/average-rating?strategy={strategyName}
```
**Query Parameters**:
- `strategy` (optional): Rating calculation strategy
  - `averageRatingStrategy` (default)
  - `weightedRatingStrategy`

**Response**: `double` (rating value)

## 🗃 Database Schema

### Reviews Table

```sql
CREATE TABLE reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    reviewer VARCHAR(255),
    review_text VARCHAR(500),
    rating DOUBLE NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Constraints
- **rating**: Must be between 1 and 5
- **review_text**: Maximum 500 characters
- **created_at**: Automatically set on creation
- **updated_at**: Automatically updated on modification

## 🔐 Security

### Gateway Header Authentication
The service uses a custom `GatewayHeaderAuthenticationFilter` that:
- Extracts user information from gateway headers (`X-User-Id`)
- Validates user ownership for update/delete operations
- Prevents unauthorized access to review modifications

### Security Configuration
- Custom security filter chain
- Header-based authentication
- Protection against unauthorized modifications

### Protected Operations
- **Update Review**: Only the review creator can update
- **Delete Review**: Only the review creator can delete

## 🧪 Testing

### Run All Tests
```bash
./mvnw test
```

### Test Coverage
The project includes comprehensive tests for:
- **Controller Tests**
  - `ReviewControllerTest`
  - `AnalyticsControllerTest`
- **Service Tests**
  - `ReviewServiceTest`
  - `AnalyticsServiceTest`
- **Application Tests**
  - `ReviewserviceApplicationTests`

### Test Reports
Test reports are generated in:
```
target/surefire-reports/
```

## 📁 Project Structure

```
reviewservice/
├── src/
│   ├── main/
│   │   ├── java/com/bookverse/reviewservice/
│   │   │   ├── ReviewserviceApplication.java      # Main application class
│   │   │   ├── config/                            # Configuration classes
│   │   │   │   ├── GatewayHeaderAuthenticationFilter.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/                        # REST controllers
│   │   │   │   ├── AnalyticsController.java
│   │   │   │   └── ReviewController.java
│   │   │   ├── dto/                               # Data Transfer Objects
│   │   │   │   ├── BookResponseDto.java
│   │   │   │   ├── ReviewRequestDto.java
│   │   │   │   └── ReviewResponseDto.java
│   │   │   ├── exception/                         # Custom exceptions
│   │   │   ├── feign/                             # Feign clients
│   │   │   │   └── BookServiceClient.java
│   │   │   ├── mapper/                            # Entity-DTO mappers
│   │   │   ├── model/                             # JPA entities
│   │   │   │   └── Review.java
│   │   │   ├── repository/                        # Data repositories
│   │   │   │   └── ReviewRepository.java
│   │   │   └── service/                           # Business logic
│   │   │       ├── AnalyticsService.java
│   │   │       ├── ReviewService.java
│   │   │       └── strategy/                      # Strategy pattern implementations
│   │   │           ├── RatingStrategy.java
│   │   │           ├── AverageRatingStrategy.java
│   │   │           └── WeightedRatingStrategy.java
│   │   └── resources/
│   │       ├── application.properties             # Application configuration
│   │       └── db/migration/                      # Flyway migrations
│   │           └── V1__create_reviews_table.sql
│   └── test/
│       └── java/com/bookverse/reviewservice/      # Test classes
│           ├── controller/
│           └── service/
├── pom.xml                                         # Maven configuration
└── README.md                                       # This file
```

## 🎨 Design Patterns

### Strategy Pattern
Used for rating calculation strategies:
- **Interface**: `RatingStrategy`
- **Implementations**:
  - `AverageRatingStrategy`: Simple average of all ratings
  - `WeightedRatingStrategy`: Weighted calculation based on custom logic

### Benefits:
- Easy to add new rating strategies
- Runtime strategy selection
- Open/Closed principle compliance

### Repository Pattern
- Clean separation between business logic and data access
- JPA repository abstraction

### DTO Pattern
- Separation between domain models and API contracts
- Request/Response DTOs for clean API design

## 🤝 Contributing

### Code Style
- Follow Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Write comprehensive unit tests
- Add JavaDoc for public methods

### Branching Strategy
- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: New features
- `bugfix/*`: Bug fixes

### Pull Request Process
1. Create a feature branch
2. Write tests for new functionality
3. Ensure all tests pass
4. Update documentation
5. Submit PR for review

## 📝 License

This project is part of the BookVerse platform.

## 📧 Contact

For questions or support, please contact the BookVerse development team.

---

**Last Updated**: November 2025  
**Version**: 0.0.1-SNAPSHOT

