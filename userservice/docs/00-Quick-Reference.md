# Quick Reference Guide

## JWT Authentication System - Cheat Sheet

---

## Component Roles

| Component | Purpose | Key Responsibility |
|-----------|---------|-------------------|
| **SecurityConfig** | Security rules | Defines who can access what |
| **JwtUtil** | Token operations | Generate, validate, extract from JWT |
| **JwtAuthenticationFilter** | Request interception | Extract token, set authentication |
| **CustomUserDetailsService** | User loading | Load user from database |
| **AuthenticationManager** | Credential verification | Validate username/password |
| **PasswordEncoder** | Password security | Hash and verify passwords |

---

## Request Flow Quick Reference

### Public Endpoint (Login/Signup)
```
Request → JwtFilter (skip) → FilterSecurity (permitAll) → Controller ✅
```

### Protected Endpoint (No Token)
```
Request → JwtFilter (no token) → FilterSecurity (needs auth) → 401 ❌
```

### Protected Endpoint (Valid Token)
```
Request → JwtFilter (set auth) → FilterSecurity (check role) → Controller ✅/403 ❌
```

### Protected Endpoint (Expired Token)
```
Request → JwtFilter (invalid) → FilterSecurity (needs auth) → 401 ❌
```

---

## Key Methods Cheat Sheet

### JwtUtil
```java
// Generate token
String token = jwtUtil.generateToken(userDetails);

// Validate token
boolean valid = jwtUtil.validateToken(token);

// Extract username
String username = jwtUtil.extractUsername(token);

// Extract authorities
List<GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);
```

### CustomUserDetailsService
```java
// Load user from database
UserDetails user = customUserDetailsService.loadUserByUsername(username);
```

### AuthenticationManager
```java
// Authenticate credentials
Authentication auth = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(username, password)
);
```

### PasswordEncoder
```java
// Hash password
String hashed = passwordEncoder.encode(rawPassword);

// Verify password
boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
```

### SecurityContext
```java
// Set authentication
SecurityContextHolder.getContext().setAuthentication(authToken);

// Get current authentication
Authentication auth = SecurityContextHolder.getContext().getAuthentication();

// Get current username
String username = auth.getName();
```

---

## HTTP Status Codes

| Code | Meaning | When It Happens |
|------|---------|----------------|
| 200 OK | Success | Valid request processed |
| 401 Unauthorized | Not authenticated | Missing/invalid/expired token |
| 403 Forbidden | Not authorized | Valid token but insufficient permissions |
| 404 Not Found | Resource not found | Endpoint doesn't exist |
| 500 Internal Server Error | Server error | Exception in code |

---

## Common Patterns

### Login Flow
```
1. User sends credentials
2. Load user from database
3. Verify password
4. Generate JWT token
5. Return token
```

### Protected Request Flow
```
1. Extract JWT from header
2. Validate token
3. Load user details
4. Extract authorities
5. Set SecurityContext
6. Check authorization rules
7. Allow/deny request
```

### Registration Flow
```
1. Receive user data
2. Hash password
3. Save to database
4. Return success
```

---

## Security Rules Syntax

### SecurityConfig Matchers
```java
// Allow everyone
.requestMatchers("/api/auth/**").permitAll()

// Require specific role
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// Require any role
.requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

// Require authentication
.anyRequest().authenticated()

// Specific HTTP method
.requestMatchers(HttpMethod.POST, "/api/books").hasRole("ADMIN")
```

### Role vs Authority
```java
// These are equivalent:
.hasRole("ADMIN")               // Expects "ROLE_ADMIN"
.hasAuthority("ROLE_ADMIN")     // Expects exact match

// When building UserDetails:
.roles("ADMIN")                 // Adds "ROLE_ADMIN"
.authorities("ROLE_ADMIN")      // Adds "ROLE_ADMIN"
```

---

## JWT Token Structure

### Header
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### Payload
```json
{
  "sub": "username",
  "roles": ["ROLE_ADMIN"],
  "iat": 1699123456,
  "exp": 1699127056
}
```

### Complete Token
```
eyJhbGc...HEADER.eyJzdWI...PAYLOAD.SflKxw...SIGNATURE
```

---

## Configuration Properties

### application.properties
```properties
# JWT Settings
jwt.secret=your-256-bit-secret-key-minimum-32-characters
jwt.expiration=3600000  # 1 hour in milliseconds

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/bookverse
spring.datasource.username=root
spring.datasource.password=password

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## Common Annotations

### Class Level
```java
@Configuration          // Marks configuration class
@EnableWebSecurity     // Enables Spring Security
@Service               // Service layer component
@Component             // Generic Spring component
@RestController        // REST controller
@Entity                // JPA entity
```

### Method Level
```java
@Bean                  // Creates Spring bean
@PostConstruct         // Runs after dependency injection
@Override              // Overrides parent method
@Autowired             // Dependency injection
@Value("${...}")       // Injects property value
```

### Parameter Level
```java
@RequestBody           // Maps request body to object
@PathVariable          // Extracts path variable
@RequestParam          // Extracts query parameter
@AuthenticationPrincipal // Injects current user
@NonNull               // Parameter cannot be null
```

---

## Testing Endpoints

### Using cURL

#### Signup
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "emailId": "john@example.com",
    "password": "john123",
    "role": "USER"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "john123"
  }'
```

#### Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/user/all \
  -H "Authorization: Bearer eyJhbGc..."
```

### Using Postman

1. **Signup**: POST `/api/auth/signup` with JSON body
2. **Login**: POST `/api/auth/login` with JSON body → Copy token
3. **Protected**: GET `/api/user/all` with Authorization header:
   - Type: Bearer Token
   - Token: paste from login response

---

## Debugging Checklist

### 401 Unauthorized
- [ ] Token present in Authorization header?
- [ ] Header format: `Bearer <token>`?
- [ ] Token expired? (Check expiration time)
- [ ] Token valid? (Signature correct?)
- [ ] Secret key same for generation and validation?

### 403 Forbidden
- [ ] User authenticated? (Token valid)
- [ ] User has required role?
- [ ] Role has "ROLE_" prefix?
- [ ] Security rules configured correctly?

### Token Issues
- [ ] "roles" key used (not "role")?
- [ ] Secret key at least 256 bits?
- [ ] Token not manually modified?
- [ ] Clock synchronized (for expiration)?

### Authentication Fails
- [ ] Password BCrypt-encoded in database?
- [ ] User exists in database?
- [ ] Username correct (case-sensitive)?
- [ ] CustomUserDetailsService loading user?

---

## Best Practices

### ✅ DO:
- Use HTTPS in production
- Store secret key in environment variables
- Set reasonable token expiration (1-24 hours)
- Use BCrypt for password hashing
- Validate tokens on every request
- Return generic error messages (don't reveal if username exists)
- Clear SecurityContext after request

### ❌ DON'T:
- Don't commit secret keys to Git
- Don't store passwords in plain text
- Don't log sensitive data (tokens, passwords)
- Don't store sensitive info in JWT
- Don't use weak secret keys
- Don't skip token validation
- Don't reveal whether username exists in errors

---

## Common Errors & Solutions

### "JWT signature does not match"
**Solution**: Ensure same secret key for generation and validation

### "Token expired"
**Solution**: Login again to get new token

### "User not found"
**Solution**: Check username exists in database

### "Bad credentials"
**Solution**: Verify password is correct and BCrypt-encoded in DB

### "Access Denied"
**Solution**: Check user has required role

### "Roles not working"
**Solution**: Ensure "roles" key in token (not "role")

---

## File Structure Reference

```
src/main/java/com/bookverse/userservice/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtUtil.java
│   └── JwtAuthenticationFilter.java
├── service/
│   ├── AuthService.java
│   ├── CustomUserDetailsService.java
│   └── UserCrudService.java
├── controller/
│   ├── AuthController.java
│   └── UserController.java
├── model/
│   ├── User.java
│   └── Role.java
├── repository/
│   └── UserRepository.java
├── dto/
│   ├── auth/
│   │   ├── SignUpDto.java
│   │   ├── LoginRequestDto.java
│   │   └── LoginResponseDto.java
│   └── user/
│       ├── UserRequestDto.java
│       └── UserResponseDto.java
└── exception/
    └── GlobalExceptionHandler.java
```

---

## Quick Troubleshooting Commands

### Check if application is running
```bash
curl http://localhost:8080/api/auth/login
```

### Decode JWT token
Visit: https://jwt.io and paste your token

### Check database
```sql
SELECT * FROM users;
```

### View application logs
```bash
tail -f logs/application.log
```

---

## Time Conversions

| Duration | Milliseconds |
|----------|--------------|
| 1 minute | 60000 |
| 5 minutes | 300000 |
| 15 minutes | 900000 |
| 30 minutes | 1800000 |
| 1 hour | 3600000 |
| 24 hours | 86400000 |
| 7 days | 604800000 |

---

## Useful Resources

- **Spring Security Docs**: https://docs.spring.io/spring-security/reference/
- **JWT.io**: https://jwt.io (decode tokens)
- **BCrypt Generator**: https://bcrypt-generator.com
- **Spring Boot Docs**: https://docs.spring.io/spring-boot/docs/current/reference/html/

---

This quick reference guide provides instant access to the most commonly needed information for your JWT authentication system!
