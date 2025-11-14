# Complete Authentication Flow - End-to-End Guide

## Overview
This document shows how all components work together in your JWT authentication system.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (Browser/Mobile)                  │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ HTTP Requests
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT APPLICATION                     │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Security Filter Chain                       │  │
│  │                                                           │  │
│  │  [1] JwtAuthenticationFilter                            │  │
│  │       ↓                                                  │  │
│  │  [2] UsernamePasswordAuthenticationFilter               │  │
│  │       ↓                                                  │  │
│  │  [3] FilterSecurityInterceptor                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Controllers                           │  │
│  │  • AuthController (login, signup)                        │  │
│  │  • UserController (user management)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     Services                             │  │
│  │  • AuthService (authentication logic)                    │  │
│  │  • UserCrudService (user operations)                     │  │
│  │  • CustomUserDetailsService (load users)                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   Repositories                           │  │
│  │  • UserRepository (database access)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     Database                             │  │
│  │  Table: users                                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Security Components                         │  │
│  │  • SecurityConfig (security rules)                       │  │
│  │  • JwtUtil (token operations)                            │  │
│  │  • PasswordEncoder (password hashing)                    │  │
│  │  • AuthenticationManager (authentication)                │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Flow 1: User Registration

### Request
```http
POST /api/auth/signup HTTP/1.1
Content-Type: application/json

{
  "username": "john",
  "emailId": "john@example.com",
  "password": "john123",
  "role": "USER"
}
```

### Step-by-Step Flow

```
1. Client sends registration request
        ↓
2. Spring Security Filter Chain
   → JwtAuthenticationFilter: No token, skip authentication
   → FilterSecurityInterceptor: /api/auth/** permitAll() ✅
        ↓
3. AuthController.signup()
   → Receives SignUpDto
        ↓
4. AuthService.registerUser(signUpDto)
   → UserMapper.toEntityFromSignUp(signUpDto)
   → Creates User entity
        ↓
5. Password Encoding
   → passwordEncoder.encode("john123")
   → BCrypt hashing: "$2a$10$XYZ..."
   → user.setPassword("$2a$10$XYZ...")
        ↓
6. Save to Database
   → userRepository.save(user)
   → SQL: INSERT INTO users (username, email_id, password, role, created_at, updated_at)
          VALUES ('john', 'john@example.com', '$2a$10$XYZ...', 'USER', NOW(), NOW())
        ↓
7. Return Response
   → SignUpDto returned
   → HTTP 200 OK
```

### Response
```json
{
  "username": "john",
  "emailId": "john@example.com",
  "role": "USER"
}
```

### Database State After Registration
```
users table:
┌────┬──────────┬────────────────────┬─────────────────┬──────┬─────────────────────┬─────────────────────┐
│ id │ username │ email_id           │ password        │ role │ created_at          │ updated_at          │
├────┼──────────┼────────────────────┼─────────────────┼──────┼─────────────────────┼─────────────────────┤
│ 1  │ john     │ john@example.com   │ $2a$10$XYZ...  │ USER │ 2024-11-04 10:00:00 │ 2024-11-04 10:00:00 │
└────┴──────────┴────────────────────┴─────────────────┴──────┴─────────────────────┴─────────────────────┘
```

---

## Flow 2: User Login (Authentication)

### Request
```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "john",
  "password": "john123"
}
```

### Step-by-Step Flow

```
1. Client sends login request
        ↓
2. Spring Security Filter Chain
   → JwtAuthenticationFilter: No token, skip
   → FilterSecurityInterceptor: /api/auth/** permitAll() ✅
        ↓
3. AuthController.login(loginRequestDto)
        ↓
4. AuthService.loginUser(loginRequestDto)
        ↓
5. Load User Details
   → customUserDetailsService.loadUserByUsername("john")
   → userRepository.findByUsername("john")
   → SQL: SELECT * FROM users WHERE username = 'john'
   → Found: User {id: 1, username: "john", password: "$2a$10$XYZ...", role: USER}
   → Convert to UserDetails:
     {
       username: "john",
       password: "$2a$10$XYZ...",
       authorities: [ROLE_USER],
       enabled: true,
       accountNonExpired: true,
       accountNonLocked: true,
       credentialsNonExpired: true
     }
        ↓
6. Authenticate with AuthenticationManager
   → authenticationManager.authenticate(
       new UsernamePasswordAuthenticationToken("john", "john123")
     )
        ↓
7. Authentication Process (Internal)
   → ProviderManager delegates to DaoAuthenticationProvider
   → DaoAuthenticationProvider.authenticate()
        ↓
8. Load User (Again, for verification)
   → CustomUserDetailsService.loadUserByUsername("john")
   → Returns UserDetails
        ↓
9. Password Verification
   → passwordEncoder.matches("john123", "$2a$10$XYZ...")
   → BCrypt comparison
   → Result: true ✅
        ↓
10. Create Authenticated Token
   → new UsernamePasswordAuthenticationToken(
       userDetails,
       null,
       [ROLE_USER]
     )
   → authenticated = true
        ↓
11. Generate JWT Token
   → jwtUtil.generateToken(userDetails)
        ↓
12. Extract Authorities
   → userDetails.getAuthorities() → ["ROLE_USER"]
        ↓
13. Create Claims
   → claims = {
       "roles": ["ROLE_USER"]
     }
        ↓
14. Build JWT
   → Jwts.builder()
       .claims({"roles": ["ROLE_USER"]})
       .subject("john")
       .issuedAt(NOW)
       .expiration(NOW + 1 hour)
       .signWith(secretKey)
       .compact()
        ↓
15. JWT Created
   → "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwic3ViIjoiam9obiIsImlhdCI6MTY5OTEyMzQ1NiwiZXhwIjoxNjk5MTI3MDU2fQ.signature"
        ↓
16. Return Response
   → LoginResponseDto(token: "eyJhbGc...")
   → HTTP 200 OK
```

### Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwic3ViIjoiam9obiIsImlhdCI6MTY5OTEyMzQ1NiwiZXhwIjoxNjk5MTI3MDU2fQ.signature"
}
```

### JWT Token Breakdown
```
Header (Base64 decoded):
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload (Base64 decoded):
{
  "roles": ["ROLE_USER"],
  "sub": "john",
  "iat": 1699123456,
  "exp": 1699127056
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret-key
)
```

---

## Flow 3: Accessing Protected Endpoint (with valid token)

### Request
```http
GET /api/user/all HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Step-by-Step Flow

```
1. Client sends request with JWT token
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. Extract Authorization Header
   → request.getHeader("Authorization")
   → "Bearer eyJhbGc..."
        ↓
4. Check Header Format
   → Starts with "Bearer "? ✅
   → Extract token: "eyJhbGc..."
        ↓
5. Extract Username from Token
   → jwtUtil.extractUsername(token)
   → Parse JWT
   → Extract "sub" claim
   → username = "john"
        ↓
6. Check if Already Authenticated
   → SecurityContextHolder.getContext().getAuthentication()
   → Result: null (not authenticated yet)
        ↓
7. Load User Details
   → customUserDetailsService.loadUserByUsername("john")
   → userRepository.findByUsername("john")
   → Returns UserDetails:
     {
       username: "john",
       password: "$2a$10$XYZ...",
       authorities: [ROLE_USER]
     }
        ↓
8. Validate Token
   → jwtUtil.validateToken(token)
   → Extract expiration: 1699127056
   → Compare with current time: 1699125000
   → Not expired ✅
   → Valid ✅
        ↓
9. Extract Authorities from Token
   → jwtUtil.extractAuthorities(token)
   → Parse JWT payload
   → Extract "roles" claim: ["ROLE_USER"]
   → Convert to GrantedAuthority list:
     [SimpleGrantedAuthority("ROLE_USER")]
        ↓
10. Create Authentication Object
   → new UsernamePasswordAuthenticationToken(
       userDetails,
       null,
       [ROLE_USER]
     )
        ↓
11. Set Security Context
   → SecurityContextHolder.getContext().setAuthentication(authToken)
   → Now authenticated! ✅
        ↓
12. Continue Filter Chain
   → filterChain.doFilter(request, response)
        ↓
13. FilterSecurityInterceptor (Authorization Check)
   → Path: /api/user/**
   → Required: hasRole("ADMIN")
   → User has: [ROLE_USER]
   → ROLE_USER != ROLE_ADMIN ❌
   → FORBIDDEN
        ↓
14. Return 403 Forbidden
   → User is authenticated but not authorized
```

### Response
```http
HTTP/1.1 403 Forbidden

{
  "error": "Forbidden",
  "message": "Access Denied"
}
```

---

## Flow 4: Accessing Protected Endpoint (Admin user)

### Setup: Admin User Login

#### Step 1: Admin exists in database
```sql
users table:
┌────┬──────────┬────────────────────┬─────────────────┬──────────┐
│ id │ username │ email_id           │ password        │ role     │
├────┼──────────┼────────────────────┼─────────────────┼──────────┤
│ 2  │ admin    │ admin@bookverse    │ $2a$10$ABC...  │ ADMIN    │
└────┴──────────┴────────────────────┴─────────────────┴──────────┘
```

#### Step 2: Admin logs in
```http
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

#### Step 3: Receives admin token
```json
{
  "token": "eyJhbGc...ADMIN_TOKEN"
}
```

**Token payload:**
```json
{
  "roles": ["ROLE_ADMIN"],
  "sub": "admin",
  "iat": 1699123456,
  "exp": 1699127056
}
```

### Request with Admin Token
```http
GET /api/user/all HTTP/1.1
Authorization: Bearer eyJhbGc...ADMIN_TOKEN
```

### Step-by-Step Flow

```
1. Client sends request with admin JWT token
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. Extract token: "eyJhbGc...ADMIN_TOKEN"
        ↓
4. Extract username: "admin"
        ↓
5. Load UserDetails
   → customUserDetailsService.loadUserByUsername("admin")
   → Returns UserDetails with authorities: [ROLE_ADMIN]
        ↓
6. Validate Token: ✅ Valid
        ↓
7. Extract Authorities from Token
   → ["ROLE_ADMIN"]
        ↓
8. Create Authentication
   → UsernamePasswordAuthenticationToken(admin, null, [ROLE_ADMIN])
        ↓
9. Set SecurityContext
   → Authentication stored
        ↓
10. Continue to FilterSecurityInterceptor
        ↓
11. Authorization Check
   → Path: /api/user/**
   → Required: hasRole("ADMIN")
   → User has: [ROLE_ADMIN]
   → Match! ✅
        ↓
12. Request reaches UserController
        ↓
13. UserController.getAllUsers()
   → userCrudService.getAllUsers()
   → userRepository.findAll()
   → Returns list of all users
        ↓
14. Return Response
   → HTTP 200 OK
   → List of UserResponseDto
```

### Response
```json
[
  {
    "id": 1,
    "username": "john",
    "emailId": "john@example.com",
    "role": "USER"
  },
  {
    "id": 2,
    "username": "admin",
    "emailId": "admin@bookverse.com",
    "role": "ADMIN"
  }
]
```

---

## Flow 5: Token Expiration

### Scenario: Token expires after 1 hour

```
Token created at:  10:00:00 (iat: 1699123456)
Token expires at:  11:00:00 (exp: 1699127056)
Current time:      11:30:00
```

### Request
```http
GET /api/user/all HTTP/1.1
Authorization: Bearer eyJhbGc...EXPIRED_TOKEN
```

### Step-by-Step Flow

```
1. Client sends request with expired token
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. Extract token: "eyJhbGc...EXPIRED_TOKEN"
        ↓
4. Extract username: "admin"
        ↓
5. Load UserDetails: ✅ User exists
        ↓
6. Validate Token
   → jwtUtil.validateToken(token)
   → extractExpiration(token) → 11:00:00
   → Current time: 11:30:00
   → expiration.before(now) → true
   → isTokenExpired() → true
   → validateToken() → false ❌
        ↓
7. Skip Authentication
   → if(validateToken) block not entered
   → SecurityContext remains empty
        ↓
8. Continue to FilterSecurityInterceptor
        ↓
9. Authorization Check
   → SecurityContext.getAuthentication() → null
   → No authentication found
   → Requires authentication
   → UNAUTHORIZED
        ↓
10. Return 401 Unauthorized
   → User must login again
```

### Response
```http
HTTP/1.1 401 Unauthorized

{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

### Solution
User must login again to get a new token.

---

## Flow 6: Public Endpoint Access

### Request
```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "john",
  "password": "john123"
}
```

### Step-by-Step Flow

```
1. Client sends request (no token)
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. Extract Authorization Header
   → request.getHeader("Authorization")
   → Result: null
        ↓
4. Check Header
   → authorizationHeader == null → true
   → Skip token extraction
   → username remains null
        ↓
5. Check Authentication Needed
   → username == null → true
   → if condition fails
   → Skip authentication
        ↓
6. Continue Filter Chain
   → filterChain.doFilter(request, response)
        ↓
7. FilterSecurityInterceptor
        ↓
8. Authorization Check
   → Path: /api/auth/login
   → Rule: .requestMatchers("/api/auth/**").permitAll()
   → Match! ✅
   → No authentication required
        ↓
9. Request reaches AuthController
        ↓
10. AuthController.login() executes
        ↓
11. AuthService.loginUser() executes
        ↓
12. Authentication and token generation
        ↓
13. Return JWT token
```

### Response
```json
{
  "token": "eyJhbGc...NEW_TOKEN"
}
```

---

## Security Context Lifecycle

### Per-Request Context

```
Thread 1 (Request 1):
    ↓
SecurityContextHolder (Thread-local)
    ↓
SecurityContext
    ↓
Authentication (User A)

Thread 2 (Request 2):
    ↓
SecurityContextHolder (Thread-local)
    ↓
SecurityContext
    ↓
Authentication (User B)
```

**Key Points:**
- Each request has its own thread
- SecurityContext is thread-local
- No cross-request contamination
- Context cleared after request completes

### Context Flow

```
Request starts
    ↓
SecurityContext created (empty)
    ↓
JwtAuthenticationFilter sets authentication
    ↓
SecurityContext.setAuthentication(authToken)
    ↓
Controllers/Services can access:
    → SecurityContextHolder.getContext().getAuthentication()
    ↓
Request completes
    ↓
SecurityContext cleared automatically
    ↓
Next request gets fresh context
```

---

## Authorization Hierarchy

### Security Rules in SecurityConfig

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()           // [1] Public
    .requestMatchers("/api/user/**").hasRole("ADMIN")      // [2] Admin only
    .anyRequest().authenticated()                          // [3] Any authenticated user
)
```

### Evaluation Order

```
Request: /api/auth/login
    ↓
Check [1]: /api/auth/** → MATCH → permitAll() ✅
Result: Allowed without authentication

Request: /api/user/all
    ↓
Check [1]: /api/auth/** → NO MATCH
Check [2]: /api/user/** → MATCH → hasRole("ADMIN")
    ↓
User has ROLE_ADMIN? → Yes ✅ / No ❌
Result: Allowed if admin, 403 Forbidden otherwise

Request: /api/books/123
    ↓
Check [1]: /api/auth/** → NO MATCH
Check [2]: /api/user/** → NO MATCH
Check [3]: anyRequest() → MATCH → authenticated()
    ↓
User authenticated? → Yes ✅ / No ❌
Result: Allowed if authenticated, 401 Unauthorized otherwise
```

---

## Component Interaction Summary

```
┌─────────────────────────────────────────────────────────────┐
│                     Component Interactions                   │
└─────────────────────────────────────────────────────────────┘

SecurityConfig
    ├─→ Defines security rules
    ├─→ Registers JwtAuthenticationFilter
    ├─→ Provides PasswordEncoder bean
    └─→ Provides AuthenticationManager bean

JwtAuthenticationFilter
    ├─→ Uses JwtUtil (extract, validate)
    ├─→ Uses CustomUserDetailsService (load user)
    └─→ Sets SecurityContext (authentication)

JwtUtil
    ├─→ Uses SecretKey (sign/verify)
    ├─→ Uses PasswordEncoder (indirectly via AuthManager)
    └─→ Creates/validates JWT tokens

CustomUserDetailsService
    ├─→ Uses UserRepository (database access)
    └─→ Converts User to UserDetails

AuthService
    ├─→ Uses AuthenticationManager (authenticate)
    ├─→ Uses PasswordEncoder (hash passwords)
    ├─→ Uses JwtUtil (generate tokens)
    ├─→ Uses CustomUserDetailsService (load users)
    └─→ Uses UserRepository (save users)

AuthenticationManager
    ├─→ Uses CustomUserDetailsService (load user)
    └─→ Uses PasswordEncoder (verify password)
```

---

## Error Scenarios

### 1. Wrong Password
```
Input: Correct username, wrong password
    ↓
CustomUserDetailsService.loadUserByUsername() ✅
    ↓
passwordEncoder.matches(wrongPassword, hashedPassword) → false
    ↓
AuthenticationManager throws BadCredentialsException
    ↓
Result: 401 Unauthorized
```

### 2. User Not Found
```
Input: Non-existent username
    ↓
CustomUserDetailsService.loadUserByUsername()
    ↓
userRepository.findByUsername() → null
    ↓
throw UsernameNotFoundException
    ↓
Converted to BadCredentialsException
    ↓
Result: 401 Unauthorized (same as wrong password)
```

### 3. Tampered Token
```
Token: Modified payload (changed role)
    ↓
JwtAuthenticationFilter extracts token
    ↓
jwtUtil.extractAllClaims()
    ↓
Verify signature → FAIL (signature doesn't match modified payload)
    ↓
Throws SignatureException
    ↓
Caught, authentication skipped
    ↓
Result: 401 Unauthorized
```

### 4. Missing Token
```
Request to protected endpoint without token
    ↓
JwtAuthenticationFilter: No Authorization header
    ↓
Skip authentication
    ↓
SecurityContext empty
    ↓
FilterSecurityInterceptor: requires authentication
    ↓
Result: 401 Unauthorized
```

### 5. Insufficient Permissions
```
USER token accessing ADMIN endpoint
    ↓
JwtAuthenticationFilter: Sets authentication with ROLE_USER
    ↓
FilterSecurityInterceptor: Requires ROLE_ADMIN
    ↓
User has ROLE_USER ≠ ROLE_ADMIN
    ↓
Result: 403 Forbidden
```

---

## Summary

This complete flow shows how:

1. **Registration**: User created with hashed password
2. **Login**: Credentials verified, JWT token generated
3. **Authentication**: Token validated on each request
4. **Authorization**: User roles checked against endpoint requirements
5. **Security Context**: Thread-local authentication storage
6. **Error Handling**: Consistent responses for security issues

All components work together to create a secure, stateless, JWT-based authentication system!

