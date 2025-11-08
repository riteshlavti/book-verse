# JwtUtil - Complete Guide

## Overview
`JwtUtil` is a utility class that handles all JWT (JSON Web Token) operations:
- **Generating** tokens when users log in
- **Validating** tokens on subsequent requests
- **Extracting** information (username, roles) from tokens

---

## What is JWT?

### Structure
A JWT has 3 parts separated by dots:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXX0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
        HEADER                          PAYLOAD                                    SIGNATURE
```

### Parts Explained

#### 1. Header (Algorithm & Token Type)
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

#### 2. Payload (Claims/Data)
```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "iat": 1699123456,
  "exp": 1699127056
}
```

#### 3. Signature (Verification)
```
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret-key
)
```

---

## Complete Code

```java
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretKey;

    @Getter
    private SecretKey signKey;

    @PostConstruct
    public void init() {
        signKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Value("${jwt.expiration}")
    private long tokenExpiration;

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Generic method to extract any claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Parse and extract all claims from token
    private Claims extractAllClaims(String token) {
        JwtParser parser = Jwts.parser().verifyWith(getSignKey()).build();
        Jws<Claims> jws = parser.parseSignedClaims(token);
        return jws.getPayload();
    }

    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Get authorities from UserDetails
    public List<String> getAuthorities(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    // Generate JWT token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", getAuthorities(userDetails));
        return createToken(claims, userDetails.getUsername());
    }

    // Create the actual token
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSignKey())
                .compact();
    }

    // Validate token
    public Boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    // Extract authorities from token
    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = claims.get("roles", List.class);

        if (roles == null) return List.of();

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .map(authority -> (GrantedAuthority) authority)
                .collect(Collectors.toList());
    }
}
```

---

## Configuration Properties

### In `application.properties`:
```properties
jwt.secret=your-256-bit-secret-key-here-make-it-long-enough
jwt.expiration=3600000
```

### Explanation:

| Property | Value | Meaning |
|----------|-------|---------|
| `jwt.secret` | 256-bit string | Secret key for signing tokens |
| `jwt.expiration` | 3600000 | Token expiration time in milliseconds (1 hour) |

### Secret Key Requirements:
- **Minimum length**: 256 bits (32 characters)
- **Randomness**: Use cryptographically secure random generation
- **Storage**: Keep in environment variables, not in code!

**Example Generation:**
```java
// Generate a secure secret key
String secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)
                       .getEncoded()
                       .toString();
```

---

## Initialization

### `@PostConstruct` Method

```java
@PostConstruct
public void init() {
    signKey = Keys.hmacShaKeyFor(secretKey.getBytes());
}
```

**When it runs:**
- After dependency injection
- Before any methods are called
- Only once during bean creation

**What it does:**
- Converts string secret key to cryptographic `SecretKey` object
- Uses HMAC-SHA256 algorithm
- Stores for reuse in signing/verification

**Why needed:**
- JWT library requires `SecretKey` object, not plain string
- Computing once is more efficient than on every operation

---

## Core Methods Explained

### 1. Generate Token

```java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", getAuthorities(userDetails));
    return createToken(claims, userDetails.getUsername());
}
```

**Flow:**
```
Input: UserDetails (from CustomUserDetailsService)
    ↓
Extract authorities: ["ROLE_ADMIN"]
    ↓
Create claims map: {"roles": ["ROLE_ADMIN"]}
    ↓
Call createToken(claims, "admin")
    ↓
Build JWT with:
    - Subject: "admin"
    - Claims: {"roles": ["ROLE_ADMIN"]}
    - IssuedAt: current time
    - Expiration: current time + 1 hour
    - Signature: HMAC-SHA256
    ↓
Return: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Real Example:**
```java
// User logs in as admin
UserDetails admin = User.withUsername("admin")
                       .password("...")
                       .roles("ADMIN")
                       .build();

String token = jwtUtil.generateToken(admin);
// Result: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNjk5MTIzNDU2LCJleHAiOjE2OTkxMjcwNTZ9.signature"
```

**Decoded Payload:**
```json
{
  "roles": ["ROLE_ADMIN"],
  "sub": "admin",
  "iat": 1699123456,
  "exp": 1699127056
}
```

---

### 2. Create Token (Internal)

```java
private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
            .signWith(getSignKey())
            .compact();
}
```

**Step by Step:**

1. **Claims**: Add custom data (roles)
2. **Subject**: Add username
3. **IssuedAt**: Add current timestamp
4. **Expiration**: Add expiration timestamp (now + 1 hour)
5. **Sign**: Sign with HMAC-SHA256 and secret key
6. **Compact**: Convert to string format

**Claims vs Subject:**
- **Subject (`sub`)**: Standard claim for user identifier
- **Custom claims**: Any additional data you want to include

---

### 3. Extract Username

```java
public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
}
```

**Flow:**
```
Token: "eyJhbGc..."
    ↓
extractClaim(token, Claims::getSubject)
    ↓
extractAllClaims(token)
    ↓
Parse and verify token
    ↓
Get payload claims
    ↓
Apply Claims::getSubject
    ↓
Return: "admin"
```

**Usage:**
```java
String token = "eyJhbGc...";
String username = jwtUtil.extractUsername(token);
// username = "admin"
```

---

### 4. Extract All Claims

```java
private Claims extractAllClaims(String token) {
    JwtParser parser = Jwts.parser().verifyWith(getSignKey()).build();
    Jws<Claims> jws = parser.parseSignedClaims(token);
    return jws.getPayload();
}
```

**What it does:**

1. **Creates parser** with secret key
2. **Parses token** (splits into header, payload, signature)
3. **Verifies signature** using secret key
4. **Returns payload** if signature is valid

**Security:**
- If token is tampered with → signature verification fails
- If secret key is wrong → verification fails
- If token is malformed → parsing fails

**Example:**
```java
Claims claims = extractAllClaims(token);
String username = claims.getSubject();           // "admin"
Date expiration = claims.getExpiration();        // 2024-11-04 21:00:00
List<String> roles = claims.get("roles", List.class); // ["ROLE_ADMIN"]
```

---

### 5. Validate Token

```java
public Boolean validateToken(String token) {
    return !isTokenExpired(token);
}
```

**Validation Steps:**

1. **Extract expiration** from token
2. **Compare** with current time
3. **Return** true if not expired

**Note:** Also validates during parsing (signature verification)

**Example:**
```java
String token = "eyJhbGc...";

if (jwtUtil.validateToken(token)) {
    // Token is valid and not expired
    // Proceed with authentication
} else {
    // Token expired
    // Return 401 Unauthorized
}
```

---

### 6. Extract Authorities

```java
public List<GrantedAuthority> extractAuthorities(String token) {
    Claims claims = extractAllClaims(token);
    List<String> roles = claims.get("roles", List.class);

    if (roles == null) return List.of();

    return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .map(authority -> (GrantedAuthority) authority)
            .collect(Collectors.toList());
}
```

**Flow:**
```
Token: "eyJhbGc..."
    ↓
Extract all claims
    ↓
Get "roles" claim: ["ROLE_ADMIN"]
    ↓
Stream: ["ROLE_ADMIN"]
    ↓
Map to SimpleGrantedAuthority: [SimpleGrantedAuthority("ROLE_ADMIN")]
    ↓
Cast to GrantedAuthority: [GrantedAuthority]
    ↓
Collect to List
    ↓
Return: List<GrantedAuthority>
```

**Why Important:**
- Spring Security uses `GrantedAuthority` for authorization
- Needed to set authorities in `SecurityContext`
- Used by `hasRole()` and `hasAuthority()` checks

**Usage in JwtAuthenticationFilter:**
```java
List<GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);
UsernamePasswordAuthenticationToken authToken =
    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
SecurityContextHolder.getContext().setAuthentication(authToken);
```

---

## Complete Token Lifecycle

### 1. Token Generation (Login)

```
User logs in
    ↓
POST /api/auth/login
    ↓
AuthService.loginUser()
    ↓
authenticationManager.authenticate() ✓
    ↓
Load UserDetails: {username: "admin", authorities: ["ROLE_ADMIN"]}
    ↓
jwtUtil.generateToken(userDetails)
    ↓
Create claims: {"roles": ["ROLE_ADMIN"]}
    ↓
Build JWT:
    - Header: {"alg": "HS256", "typ": "JWT"}
    - Payload: {"sub": "admin", "roles": ["ROLE_ADMIN"], "iat": ..., "exp": ...}
    - Signature: HMACSHA256(header + payload, secret)
    ↓
Return token: "eyJhbGc..."
    ↓
Client stores token
```

### 2. Token Usage (Protected Request)

```
Client makes request
    ↓
GET /api/user/all
Header: Authorization: Bearer eyJhbGc...
    ↓
JwtAuthenticationFilter intercepts
    ↓
Extract token from header
    ↓
jwtUtil.extractUsername(token) → "admin"
    ↓
jwtUtil.validateToken(token) → true
    ↓
jwtUtil.extractAuthorities(token) → [ROLE_ADMIN]
    ↓
CustomUserDetailsService.loadUserByUsername("admin")
    ↓
Create Authentication object with authorities
    ↓
Set SecurityContext
    ↓
SecurityFilterChain checks: hasRole("ADMIN") ✓
    ↓
Request reaches controller
```

### 3. Token Expiration

```
Token created at: 20:00:00
Token expires at: 21:00:00 (1 hour later)

Request at 20:30:00 → ✅ Valid
Request at 21:15:00 → ❌ Expired (401 Unauthorized)
```

---

## Security Considerations

### ✅ Best Practices

1. **Secret Key Security**
   - Use environment variables
   - Never commit to Git
   - Minimum 256 bits
   - Rotate periodically

2. **Token Expiration**
   - Set reasonable expiration (1-24 hours)
   - Shorter for sensitive operations
   - Use refresh tokens for longer sessions

3. **Claims**
   - Don't store sensitive data (passwords, SSN, etc.)
   - Keep payload small for performance
   - Only store necessary information

4. **Validation**
   - Always verify signature
   - Check expiration
   - Validate issuer if needed

### ❌ Common Mistakes

1. **Weak Secret Key**
   ```java
   // ❌ DON'T
   jwt.secret=secret
   
   // ✅ DO
   jwt.secret=d1e8a70b4c3f2a9e5b6d8f0c2a4b6e8d9f1a3c5e7b9d0f2a4c6e8f0a2b4d6e8f
   ```

2. **Storing Secrets in Code**
   ```java
   // ❌ DON'T
   private String secretKey = "my-secret-key";
   
   // ✅ DO
   @Value("${jwt.secret}")
   private String secretKey;
   ```

3. **Not Checking Expiration**
   ```java
   // ❌ DON'T
   public Boolean validateToken(String token) {
       return true; // Always valid!
   }
   
   // ✅ DO
   public Boolean validateToken(String token) {
       return !isTokenExpired(token);
   }
   ```

4. **Storing Passwords in JWT**
   ```java
   // ❌ DON'T
   claims.put("password", user.getPassword());
   
   // ✅ DO
   claims.put("roles", user.getRoles());
   ```

---

## Troubleshooting

### Issue: "JWT signature does not match"
**Cause**: Secret key mismatch or token tampering
**Solution**: Ensure same secret key used for generation and validation

### Issue: "Token expired"
**Cause**: Token older than expiration time
**Solution**: Generate new token (login again) or implement refresh token

### Issue: "Null pointer when extracting roles"
**Cause**: Token generated with wrong key name
**Solution**: Ensure "roles" key is used consistently in generation and extraction

### Issue: "Secret key too short"
**Cause**: Secret key less than 256 bits
**Solution**: Use minimum 32-character secret key

---

## Testing

### Example Test Cases

```java
@Test
public void testGenerateToken() {
    UserDetails user = User.withUsername("admin")
                          .password("password")
                          .roles("ADMIN")
                          .build();
    
    String token = jwtUtil.generateToken(user);
    
    assertNotNull(token);
    assertTrue(token.split("\\.").length == 3); // Header.Payload.Signature
}

@Test
public void testExtractUsername() {
    String token = jwtUtil.generateToken(userDetails);
    String username = jwtUtil.extractUsername(token);
    
    assertEquals("admin", username);
}

@Test
public void testValidateToken() {
    String token = jwtUtil.generateToken(userDetails);
    assertTrue(jwtUtil.validateToken(token));
}

@Test
public void testExpiredToken() throws InterruptedException {
    // Set expiration to 1 second
    jwtUtil.tokenExpiration = 1000;
    String token = jwtUtil.generateToken(userDetails);
    
    Thread.sleep(2000); // Wait 2 seconds
    
    assertFalse(jwtUtil.validateToken(token));
}
```

---

## Summary

**JwtUtil** is your JWT Swiss Army knife:

1. **Generates** secure tokens with user roles
2. **Validates** tokens on every request
3. **Extracts** username and authorities for authorization
4. **Signs** tokens with HMAC-SHA256 for security
5. **Manages** token expiration

It's the foundation of your stateless authentication system!

