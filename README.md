# BookVerse Microservices Project Overview 📚✨

BookVerse is a scalable, secure, and modular microservices-based system for managing books, users, and reviews. The architecture consists of four main services:

- **Gateway Service** 🚪
  - Single entry point for all client requests
  - Routes traffic to backend services
  - Enforces authentication and authorization (JWT tokens)
  - Applies security policies
  - Centralized logging and error handling

- **Book Service** 📖
  - Manages book-related operations:
    - List available books
    - Search books by criteria
    - Add new books
    - Update book details
  - Exposes RESTful APIs
  - Interacts with a database for book data
  - Utilizes **CompletableFuture** for asynchronous processing, enabling non-blocking operations and improving performance for concurrent tasks ⚡
  - Uses **Flyway** for database migration and version control (shared with User and Review services) 🛠️

- **User Service** 👤
  - Handles user account management:
    - Registration
    - Login
    - Authentication
    - Profile updates
  - Securely stores user credentials
  - Generates authentication tokens
  - Provides user data to other services
  - Uses **Flyway** for database migration and version control 🛠️

- **Review Service** 📝
  - Manages book reviews:
    - Add, view, edit, delete reviews
    - Link reviews to users and books
    - Enable feedback and ratings ⭐
  - Ensures only authenticated users can post/modify reviews
  - Provides APIs for retrieving reviews by book or user
  - Uses **Flyway** for database migration and version control 🛠️

**Technologies Used:**
- Spring Boot for rapid development 🚀
- Spring Cloud Gateway for routing 🌐
- Eureka for service discovery 🔎
- Spring Security for endpoint protection 🔒
- Maven for build and dependency management 🧩
- **Flyway** for automated database migration and version control in Book, User, and Review services 🛠️

**Architecture Highlights:**
- All services registered with Eureka for dynamic scaling and communication 🔄
- Modular approach: each service can be developed, deployed, and scaled independently 🏗️
- Supports future growth and feature expansion for BookVerse 🌱
