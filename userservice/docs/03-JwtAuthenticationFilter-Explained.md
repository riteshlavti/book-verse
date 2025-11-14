# JwtAuthenticationFilter - Complete Guide

## Overview
`JwtAuthenticationFilter` is a custom Spring Security filter that:
- Intercepts EVERY HTTP request
- Extracts JWT token from Authorization header
- Validates the token
- Sets authentication in Spring Security context
- Allows the request to proceed

**Position in Filter Chain**: Runs BEFORE standard authentication filters

---

## Complete Code

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                   @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) 
                                   throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(token);
        }

        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if(jwtUtil.validateToken(token)) {
                List<GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

---

## Why Extend OncePerRequestFilter?

### Problem: Regular filters can run multiple times
```
Request → Filter 1 → Filter 2 → Forward → Filter 1 (again!) → Servlet
```

### Solution: OncePerRequestFilter
- Guarantees filter runs **exactly once** per request
- Even with forwards/includes
- Prevents duplicate authentication attempts

### Alternatives:

| Filter Type | Runs | Use Case |
|-------------|------|----------|
| `Filter` | Multiple times | Basic filtering |
| `OncePerRequestFilter` | Once per request | Authentication, logging |
| `GenericFilterBean` | Multiple times | Spring-aware filters |

---

## Step-by-Step Breakdown

### Step 1: Extract Authorization Header

```java
String authorizationHeader = request.getHeader("Authorization");
String token = null;
String username = null;
```

**What it does:**
- Gets the `Authorization` header from HTTP request
- Initializes token and username variables

**Example Request:**
```http
GET /api/user/all HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Result:**
```java
authorizationHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Step 2: Extract Token from Header

```java
if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
    token = authorizationHeader.substring(7);
    username = jwtUtil.extractUsername(token);
}
```

**Checks:**
1. Header exists (not null)
2. Header starts with "Bearer "

**Token Extraction:**
```
"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        ↑
Position 7 (after "Bearer ")
        ↓
"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Why "Bearer"?**
- Standard OAuth 2.0 authentication scheme
- Indicates token-based authentication
- Format: `Authorization: Bearer <token>`

**Username Extraction:**
```java
username = jwtUtil.extractUsername(token);
// Parses JWT and extracts "sub" claim
// Result: "admin"
```

---

### Step 3: Check if Authentication Needed

```java
if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
    // Authenticate user
}
```

**Conditions:**

1. **`username != null`**
    - Token was successfully extracted
    - Token contains valid username

2. **`SecurityContextHolder.getContext().getAuthentication() == null`**
    - User is not already authenticated
    - Prevents redundant authentication

**Why check authentication?**
- Avoid re-authenticating already authenticated users
- Performance optimization
- Filter may run multiple times in complex scenarios

**SecurityContextHolder:**
```
SecurityContextHolder (Thread-local storage)
    └─→ SecurityContext
            └─→ Authentication (current user's authentication info)
```

---

### Step 4: Load User Details

```java
UserDetails userDetails = userDetailsService.loadUserByUsername(username);
```

**What it does:**
- Calls your `CustomUserDetailsService`
- Queries database for user by username
- Returns `UserDetails` with user info

**Flow:**
```
username = "admin"
    ↓
CustomUserDetailsService.loadUserByUsername("admin")
    ↓
UserRepository.findByUsername("admin")
    ↓
Query database: SELECT * FROM users WHERE username = 'admin'
    ↓
User found: {id: 1, username: "admin", password: "$2a$10...", role: ADMIN}
    ↓
Convert to UserDetails:
    ↓
Return: User {
    username: "admin",
    password: "$2a$10...",
    authorities: [ROLE_ADMIN],
    enabled: true,
    ...
}
```

**Why load from database?**
- Verify user still exists
- Get latest user information
- Ensure user is not disabled/locked

---

### Step 5: Validate Token

```java
if(jwtUtil.validateToken(token)) {
    // Token is valid
}
```

**Validation Checks:**
1. **Signature verification**: Token not tampered with
2. **Expiration check**: Token not expired
3. **Structure check**: Token is well-formed

**Internal Process:**
```java
// In JwtUtil:
public Boolean validateToken(String token) {
    try {
        Date expiration = extractExpiration(token);
        return !expiration.before(new Date());
    } catch (Exception e) {
        return false; // Invalid token
    }
}
```

**Possible Outcomes:**
- ✅ `true`: Token valid, proceed with authentication
- ❌ `false`: Token invalid/expired, skip authentication (401 later)

---

### Step 6: Extract Authorities from Token

```java
List<GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);
```

**What it does:**
- Extracts "roles" claim from JWT
- Converts to Spring Security's `GrantedAuthority` list

**Example:**
```
Token payload: {"sub": "admin", "roles": ["ROLE_ADMIN", "ROLE_USER"], ...}
    ↓
Extract "roles" claim: ["ROLE_ADMIN", "ROLE_USER"]
    ↓
Map to SimpleGrantedAuthority:
    ↓
[SimpleGrantedAuthority("ROLE_ADMIN"), SimpleGrantedAuthority("ROLE_USER")]
    ↓
Cast to List<GrantedAuthority>
    ↓
Result: [ROLE_ADMIN, ROLE_USER]
```

**Why from token, not database?**
- Token contains roles at the time of generation
- Stateless: no need to query database on every request
- Performance: faster than database lookup

**Trade-off:**
- ✅ Fast, scalable
- ❌ Roles not updated until new token generated

---

### Step 7: Create Authentication Token

```java
UsernamePasswordAuthenticationToken authToken =
    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
```

**Parameters:**

| Parameter | Value | Meaning |
|-----------|-------|---------|
| `principal` | `userDetails` | The authenticated user |
| `credentials` | `null` | No credentials needed (already authenticated) |
| `authorities` | `authorities` | User's roles/permissions |

**Why UsernamePasswordAuthenticationToken?**
- Standard authentication object in Spring Security
- Indicates successful authentication
- Contains all info needed for authorization

**Authentication States:**

```java
// Before authentication (from login form):
new UsernamePasswordAuthenticationToken(username, password);
// authenticated = false

// After authentication (this filter):
new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
// authenticated = true
```

---

### Step 8: Set Security Context

```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

**What it does:**
- Stores authentication in thread-local storage
- Makes user info available to entire request processing
- Used by Spring Security for authorization decisions

**Visual Representation:**
```
Thread (Handling HTTP Request)
    │
    └─→ ThreadLocal Storage
            │
            └─→ SecurityContextHolder
                    │
                    └─→ SecurityContext
                            │
                            └─→ Authentication
                                    ├─→ Principal: admin
                                    ├─→ Credentials: null
                                    └─→ Authorities: [ROLE_ADMIN]
```

**Impact:**
- Controllers can access current user
- `@PreAuthorize` annotations work
- `hasRole()` in SecurityConfig works

**Usage in Controllers:**
```java
@GetMapping("/current")
public String getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth.getName(); // "admin"
}

// Or using Spring's annotation:
@GetMapping("/current")
public String getCurrentUser(@AuthenticationPrincipal UserDetails user) {
    return user.getUsername(); // "admin"
}
```

---

### Step 9: Continue Filter Chain

```java
filterChain.doFilter(request, response);
```

**What it does:**
- Passes request to next filter in chain
- Eventually reaches your controller
- **MUST be called**, or request hangs!

**Filter Chain Flow:**
```
Request
    ↓
[1] JwtAuthenticationFilter ← You are here
    ↓
    └─→ Set authentication ✓
            ↓
    └─→ filterChain.doFilter(request, response)
                ↓
[2] UsernamePasswordAuthenticationFilter
                ↓
[3] FilterSecurityInterceptor (checks authorization)
                ↓
[4] Your Controller (UserController)
                ↓
Response
```

**If you forget to call:**
```java
// ❌ WRONG:
if (jwtUtil.validateToken(token)) {
    // Set authentication
    return; // Request never proceeds!
}

// ✅ CORRECT:
if (jwtUtil.validateToken(token)) {
    // Set authentication
}
filterChain.doFilter(request, response); // Always call
```

---

## Complete Request Flow

### Scenario 1: Valid Token

```
1. GET /api/user/all
   Header: Authorization: Bearer eyJhbGc...
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. Extract header: "Bearer eyJhbGc..."
        ↓
4. Extract token: "eyJhbGc..."
        ↓
5. Extract username: "admin"
        ↓
6. Check SecurityContext: no authentication yet
        ↓
7. Load UserDetails from DB: User(username=admin)
        ↓
8. Validate token: ✅ Valid
        ↓
9. Extract authorities: [ROLE_ADMIN]
        ↓
10. Create authentication token
        ↓
11. Set SecurityContext with authentication
        ↓
12. Continue filter chain
        ↓
13. FilterSecurityInterceptor checks: hasRole("ADMIN") ✅
        ↓
14. UserController.getAllUsers() executes
        ↓
15. Return response
```

### Scenario 2: No Token

```
1. GET /api/user/all
   (No Authorization header)
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. Extract header: null
        ↓
4. Skip token extraction
        ↓
5. username = null
        ↓
6. Skip authentication (if condition fails)
        ↓
7. Continue filter chain
        ↓
8. FilterSecurityInterceptor checks: hasRole("ADMIN") ❌
        ↓
9. No authentication in SecurityContext
        ↓
10. Return 401 Unauthorized
```

### Scenario 3: Invalid/Expired Token

```
1. GET /api/user/all
   Header: Authorization: Bearer expired_token
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. Extract header: "Bearer expired_token"
        ↓
4. Extract token: "expired_token"
        ↓
5. Extract username: "admin"
        ↓
6. Load UserDetails: User(username=admin)
        ↓
7. Validate token: ❌ Expired
        ↓
8. Skip setting authentication
        ↓
9. Continue filter chain
        ↓
10. FilterSecurityInterceptor: no authentication ❌
        ↓
11. Return 401 Unauthorized
```

### Scenario 4: Public Endpoint

```
1. POST /api/auth/login
   Body: {username, password}
        ↓
2. JwtAuthenticationFilter.doFilterInternal()
        ↓
3. No Authorization header
        ↓
4. Skip authentication
        ↓
5. Continue filter chain
        ↓
6. FilterSecurityInterceptor checks: /api/auth/** permitAll() ✅
        ↓
7. AuthController.login() executes (no authentication needed)
        ↓
8. Return new JWT token
```

---

## Integration with Other Components

### 1. With SecurityConfig

```java
// In SecurityConfig:
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

**Why before?**
- JWT filter sets authentication first
- Username/Password filter sees authentication is already set
- Skips unnecessary processing

### 2. With JwtUtil

```java
@Autowired
private JwtUtil jwtUtil;

// Used for:
username = jwtUtil.extractUsername(token);        // Get username
boolean valid = jwtUtil.validateToken(token);     // Validate
authorities = jwtUtil.extractAuthorities(token);  // Get roles
```

### 3. With CustomUserDetailsService

```java
@Autowired
private CustomUserDetailsService userDetailsService;

// Used for:
UserDetails userDetails = userDetailsService.loadUserByUsername(username);
```

**Why needed?**
- Token contains username and roles, but not full user object
- Need full `UserDetails` for authentication object
- Verifies user still exists in database

---

## Common Issues & Solutions

### Issue 1: "Authentication not set"

**Symptom**: 401 Unauthorized despite valid token

**Possible Causes:**
```java
// ❌ Token validation failing silently
if(jwtUtil.validateToken(token)) {
    // Never reaches here
}

// ❌ Authorities not extracted
authorities = jwtUtil.extractAuthorities(token); // Returns empty list

// ❌ Wrong header format
Authorization: eyJhbGc... // Missing "Bearer "
```

**Solutions:**
1. Check token expiration
2. Verify "roles" claim exists in token
3. Ensure header format: `Authorization: Bearer <token>`

---

### Issue 2: "Filter runs multiple times"

**Symptom**: Same request authenticated multiple times

**Cause**: Using `Filter` instead of `OncePerRequestFilter`

**Solution:**
```java
// ✅ Use OncePerRequestFilter
public class JwtAuthenticationFilter extends OncePerRequestFilter
```

---

### Issue 3: "Request hangs"

**Symptom**: Request never completes

**Cause**: Forgot to call `filterChain.doFilter()`

**Solution:**
```java
// ✅ Always call at the end
@Override
protected void doFilterInternal(...) {
    // ... authentication logic ...
    
    filterChain.doFilter(request, response); // Must call!
}
```

---

### Issue 4: "Roles not working"

**Symptom**: `hasRole("ADMIN")` fails despite correct token

**Possible Causes:**
```java
// ❌ Wrong key in token
claims.put("role", roles);  // Should be "roles"

// ❌ Authorities not set in authentication
new UsernamePasswordAuthenticationToken(userDetails, null, null); // Missing authorities

// ❌ Role prefix mismatch
// Token has: "ADMIN"
// Spring expects: "ROLE_ADMIN"
```

**Solutions:**
1. Use "roles" key consistently
2. Pass authorities to authentication token
3. Ensure "ROLE_" prefix in token

---

## Security Considerations

### ✅ Best Practices

1. **Always validate token**
   ```java
   if(jwtUtil.validateToken(token)) {
       // Only authenticate if valid
   }
   ```

2. **Check user exists**
   ```java
   UserDetails userDetails = userDetailsService.loadUserByUsername(username);
   // Throws exception if user not found/disabled
   ```

3. **Use HTTPS in production**
    - Prevents token interception
    - Required for security

4. **Handle exceptions gracefully**
   ```java
   try {
       username = jwtUtil.extractUsername(token);
   } catch (Exception e) {
       // Invalid token, skip authentication
   }
   ```

### ❌ Common Mistakes

1. **Not validating token**
   ```java
   // ❌ DON'T
   username = jwtUtil.extractUsername(token);
   // Directly set authentication without validation
   
   // ✅ DO
   if(jwtUtil.validateToken(token)) {
       // Then authenticate
   }
   ```

2. **Logging sensitive data**
   ```java
   // ❌ DON'T
   log.info("Token: " + token); // Token in logs!
   
   // ✅ DO
   log.info("Authenticating user: " + username);
   ```

3. **Trusting token blindly**
   ```java
   // ❌ DON'T
   authorities = jwtUtil.extractAuthorities(token);
   // What if token is forged?
   
   // ✅ DO
   if(jwtUtil.validateToken(token)) { // Verifies signature
       authorities = jwtUtil.extractAuthorities(token);
   }
   ```

---

## Testing

### Unit Test Example

```java
@Test
public void testDoFilterInternal_ValidToken() throws Exception {
    // Arrange
    String token = "valid.jwt.token";
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtUtil.extractUsername(token)).thenReturn("admin");
    when(jwtUtil.validateToken(token)).thenReturn(true);
    when(jwtUtil.extractAuthorities(token)).thenReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(userDetailsService.loadUserByUsername("admin")).thenReturn(mockUserDetails);
    
    // Act
    filter.doFilterInternal(request, response, chain);
    
    // Assert
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(auth);
    assertEquals("admin", auth.getName());
    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    verify(chain).doFilter(request, response);
}
```

---

## Summary

**JwtAuthenticationFilter** is the gatekeeper of your application:

1. **Intercepts** every request
2. **Extracts** JWT token from header
3. **Validates** token signature and expiration
4. **Loads** user details from database
5. **Extracts** authorities from token
6. **Sets** authentication in SecurityContext
7. **Passes** request to next filter

It bridges JWT tokens with Spring Security's authentication mechanism, enabling stateless, secure API authentication!

