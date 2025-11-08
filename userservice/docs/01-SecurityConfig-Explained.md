# SecurityConfig - Complete Guide

## Overview
`SecurityConfig` is the central configuration class for Spring Security in your application. It defines:
- Which endpoints are public vs protected
- How authentication and authorization work
- Password encoding strategy
- JWT filter integration

---

## Complete Code

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/user/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
```

---

## Annotations Explained

### `@Configuration`
- Marks this class as a Spring configuration class
- Tells Spring to scan this class for `@Bean` methods
- Beans defined here are registered in the Spring Application Context

### `@EnableWebSecurity`
- Activates Spring Security for the application
- Enables custom security configuration
- Auto-configures security filters and components

---

## Component 1: SecurityFilterChain

### Purpose
Defines the security rules for HTTP requests - which endpoints need authentication and what roles are required.

### Configuration Breakdown

#### 1. CSRF Disabled
```java
.csrf(csrf -> csrf.disable())
```

**What it does:**
- Disables Cross-Site Request Forgery protection

**Why:**
- Your app uses stateless JWT tokens (not cookies)
- CSRF protection is mainly needed for cookie-based sessions
- JWT in Authorization header is not vulnerable to CSRF

**Real-world analogy:**
- Like removing a lock (CSRF) from a door that's already secured by a different system (JWT)

---

#### 2. Authorization Rules
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/user/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

**Breakdown:**

| Pattern | Rule | Who Can Access | Example URLs |
|---------|------|----------------|--------------|
| `/api/auth/**` | `permitAll()` | Everyone (no token needed) | `/api/auth/login`, `/api/auth/signup` |
| `/api/user/**` | `hasRole("ADMIN")` | Only users with ROLE_ADMIN | `/api/user/all`, `/api/user/123` |
| `anyRequest()` | `authenticated()` | Any authenticated user | Any other endpoint |

**Important Notes:**
- `hasRole("ADMIN")` automatically looks for `ROLE_ADMIN` in authorities
- Spring Security adds "ROLE_" prefix automatically
- Order matters! More specific rules should come first

**Example Flow:**
```
Request: POST /api/auth/login
→ Matches: /api/auth/**
→ Rule: permitAll()
→ Result: ✅ Allowed without authentication

Request: GET /api/user/all
→ Matches: /api/user/**
→ Rule: hasRole("ADMIN")
→ Check: Does user have ROLE_ADMIN?
→ Result: ✅ Allowed if admin, ❌ 403 Forbidden if not

Request: GET /api/books/123
→ Matches: anyRequest()
→ Rule: authenticated()
→ Result: ✅ Allowed if valid JWT, ❌ 401 Unauthorized if not
```

---

#### 3. Session Management
```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**What it does:**
- Tells Spring Security NOT to create HTTP sessions
- Application is completely stateless

**Why:**
- JWT tokens contain all necessary information
- No need to store user state on the server
- Enables horizontal scaling (any server can handle any request)

**Session Policies:**
| Policy | Description | Use Case |
|--------|-------------|----------|
| `STATELESS` | No session created | JWT-based apps (your case) |
| `STATEFUL` | Session created and maintained | Traditional web apps |
| `IF_REQUIRED` | Create session if needed | Hybrid apps |
| `NEVER` | Don't create, but use if exists | Legacy migration |

---

#### 4. JWT Filter Integration
```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

**What it does:**
- Adds your custom JWT filter to the security filter chain
- Runs BEFORE Spring's default authentication filter

**Filter Chain Order:**
```
HTTP Request
    ↓
[1] JwtAuthenticationFilter ← Your custom filter (extracts & validates JWT)
    ↓
[2] UsernamePasswordAuthenticationFilter ← Spring's default filter
    ↓
[3] Other Security Filters...
    ↓
[4] Your Controller
```

**Why before?**
- JWT filter needs to run first to extract token and set authentication
- If authentication is already set, UsernamePasswordAuthenticationFilter is skipped
- Efficient processing of JWT requests

---

## Component 2: PasswordEncoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Purpose
Provides a secure way to hash and verify passwords.

### How BCrypt Works

**During Registration:**
```
Plain Password: "admin123"
    ↓
BCryptPasswordEncoder.encode("admin123")
    ↓
Generated Hash: "$2a$10$N9qo8uLOickgx2ZMRZoMye1J5U2kARLLIJn9uQy.IjJcSuH7FyGiu"
    ↓
Stored in Database
```

**During Login:**
```
User enters: "admin123"
    ↓
From database: "$2a$10$N9qo8uLOickgx2ZMRZoMye1J5U2kARLLIJn9uQy.IjJcSuH7FyGiu"
    ↓
BCryptPasswordEncoder.matches("admin123", "$2a$10$...")
    ↓
Returns: true/false
```

### BCrypt Features

1. **Salting**: Each password gets a unique random salt
2. **Rounds**: Configurable work factor (default 10)
3. **One-way**: Cannot decrypt the hash back to password
4. **Deterministic verification**: Same password + same hash = always matches

### Hash Anatomy
```
$2a$10$N9qo8uLOickgx2ZMRZoMye1J5U2kARLLIJn9uQy.IjJcSuH7FyGiu
│││ ││ │                              │
│││ ││ │                              └─ Hash (31 chars)
│││ ││ └─ Salt (22 chars)
│││ └─ Cost factor (2^10 = 1024 rounds)
││└─ Minor revision
│└─ Major revision
└─ Algorithm identifier
```

### Why This Bean?
- Makes PasswordEncoder injectable across the app
- Used in AuthService for password encoding during registration
- Used by AuthenticationManager for password verification during login

---

## Component 3: AuthenticationManager Bean

```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
        throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
}
```

### Purpose
Exposes the configured AuthenticationManager for dependency injection.

### What It Does

1. **Takes** Spring's auto-configured `AuthenticationConfiguration`
2. **Extracts** the fully configured `AuthenticationManager`
3. **Registers** it as a bean in the Spring context
4. **Enables** injection into your services (like AuthService)

### What Gets Auto-Configured

Spring Boot automatically wires:
```
AuthenticationManager
    └─→ ProviderManager
            └─→ DaoAuthenticationProvider
                    ├─→ CustomUserDetailsService (loads user from DB)
                    └─→ PasswordEncoder (verifies passwords)
```

### Usage Example in Your Code

```java
@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager; // ← Injected bean
    
    public LoginResponseDto loginUser(LoginRequestDto request) {
        // Uses the bean to authenticate
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );
        // ... generate JWT ...
    }
}
```

---

## Complete Request Flow

### Example: User Login

```
1. POST /api/auth/login
   Body: {"username": "admin", "password": "admin123"}
        ↓
2. SecurityFilterChain checks rules
   → /api/auth/** matches permitAll()
   → ✅ Request allowed to proceed
        ↓
3. AuthController receives request
        ↓
4. AuthService.loginUser() called
        ↓
5. authenticationManager.authenticate() ← Uses Bean #3
   → DaoAuthenticationProvider checks credentials
   → CustomUserDetailsService loads user from DB
   → PasswordEncoder.matches() ← Uses Bean #2
   → Returns Authentication object
        ↓
6. JWT token generated
        ↓
7. Token returned to client
```

### Example: Protected Endpoint Access

```
1. GET /api/user/all
   Header: Authorization: Bearer eyJhbGc...
        ↓
2. JwtAuthenticationFilter runs ← Added by Bean #1
   → Extracts token from header
   → Validates token
   → Extracts roles
   → Sets SecurityContext
        ↓
3. SecurityFilterChain checks rules
   → /api/user/** requires hasRole("ADMIN")
   → Checks SecurityContext for ROLE_ADMIN
   → ✅ Authorized / ❌ 403 Forbidden
        ↓
4. If authorized, UserController receives request
```

---

## Common Configurations

### Adding Multiple Roles
```java
.requestMatchers("/api/user/**").hasAnyRole("ADMIN", "MANAGER")
```

### Requiring Multiple Authorities
```java
.requestMatchers("/api/sensitive/**").hasRole("ADMIN").hasAuthority("WRITE")
```

### Different HTTP Methods
```java
.requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/books/**").hasRole("ADMIN")
```

### Ant Patterns
```java
.requestMatchers("/api/user/*").hasRole("ADMIN")      // Matches /api/user/123
.requestMatchers("/api/user/**").hasRole("ADMIN")     // Matches /api/user/123/orders
```

---

## Security Best Practices

### ✅ DO:
- Keep authentication endpoints public (`/api/auth/**`)
- Use STATELESS for JWT-based apps
- Disable CSRF for pure REST APIs
- Use specific matchers before general ones
- Use BCrypt or Argon2 for password hashing

### ❌ DON'T:
- Don't expose admin endpoints publicly
- Don't use plain text passwords
- Don't enable sessions with JWT (defeats the purpose)
- Don't forget to validate JWT tokens
- Don't log sensitive information

---

## Troubleshooting

### 401 Unauthorized
**Cause**: No valid JWT token
**Solution**: Ensure token is sent in `Authorization: Bearer <token>` header

### 403 Forbidden
**Cause**: Valid token but insufficient permissions
**Solution**: Check user has required role (e.g., ROLE_ADMIN)

### 404 Not Found (but endpoint exists)
**Cause**: Request matcher might be too restrictive
**Solution**: Check the order of matchers and patterns

### Password Not Matching
**Cause**: Stored password not BCrypt-encoded
**Solution**: Ensure passwords are encoded before saving to DB

---

## Summary

**SecurityConfig** is the heart of your security setup:

1. **SecurityFilterChain**: Defines who can access what
2. **PasswordEncoder**: Securely hashes passwords
3. **AuthenticationManager**: Validates user credentials

Together, they create a secure, stateless, JWT-based authentication system!
