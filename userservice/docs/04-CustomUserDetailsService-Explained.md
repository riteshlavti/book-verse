# CustomUserDetailsService - Complete Guide

## Overview
`CustomUserDetailsService` is a bridge between Spring Security and your database. It:
- Loads user details from your database
- Converts your `User` entity to Spring Security's `UserDetails`
- Enables Spring Security to authenticate users

**Implements**: `UserDetailsService` interface (Spring Security)

---

## Complete Code

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
```

---

## Why This Class is Needed

### The Problem

**Your Database Model:**
```java
public class User {
    private Long id;
    private String username;
    private String emailId;
    private String password;
    private Role role; // Enum: USER, ADMIN
    private Date createdAt;
    private Date updatedAt;
}
```

**Spring Security Expects:**
```java
public interface UserDetails {
    String getUsername();
    String getPassword();
    Collection<? extends GrantedAuthority> getAuthorities();
    boolean isAccountNonExpired();
    boolean isAccountNonLocked();
    boolean isCredentialsNonExpired();
    boolean isEnabled();
}
```

### The Solution
`CustomUserDetailsService` converts your `User` entity to Spring Security's `UserDetails` interface.

---

## Interface Implementation

### UserDetailsService Interface

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

**Purpose:**
- Single method interface (Functional Interface)
- Contract for loading user-specific data
- Used by Spring Security's authentication process

**When Called:**
1. During login (by `AuthenticationManager`)
2. In JWT filter (to load user details)
3. Anywhere Spring Security needs user info

---

## Step-by-Step Breakdown

### Step 1: Autowire UserRepository

```java
@Autowired
private UserRepository userRepository;
```

**What it does:**
- Injects Spring Data JPA repository
- Provides database access methods

**UserRepository:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
```

---

### Step 2: Load User from Database

```java
User user = userRepository.findByUsername(username);
```

**What happens:**
1. JPA generates SQL query
2. Database is queried
3. Result mapped to `User` entity

**Generated SQL:**
```sql
SELECT * FROM users WHERE username = ?
```

**Example:**
```java
// Input
username = "admin"

// Query executed
SELECT id, username, email_id, password, role, created_at, updated_at
FROM users
WHERE username = 'admin'

// Result
User {
    id: 1,
    username: "admin",
    emailId: "admin@bookverse.com",
    password: "$2a$10$N9qo8uLOickgx2ZMRZoMye...",
    role: ADMIN,
    createdAt: 2024-01-15 10:30:00,
    updatedAt: 2024-01-15 10:30:00
}
```

---

### Step 3: Check if User Exists

```java
if (user == null) {
    throw new UsernameNotFoundException("User not found with username: " + username);
}
```

**Why needed:**
- Username might not exist in database
- Spring Security expects exception for invalid users
- Exception triggers authentication failure

**Flow:**
```
User not found
    ↓
Throw UsernameNotFoundException
    ↓
Caught by AuthenticationManager
    ↓
Converted to BadCredentialsException
    ↓
Returns 401 Unauthorized to client
```

**Security Note:**
- Both "user not found" and "wrong password" return same error
- Prevents username enumeration attacks
- Attacker can't determine which usernames exist

---

### Step 4: Build UserDetails Object

```java
return org.springframework.security.core.userdetails.User
        .withUsername(user.getUsername())
        .password(user.getPassword())
        .roles(user.getRole().name())
        .build();
```

**Builder Pattern:**
- Fluent API for creating `UserDetails`
- Clear and readable
- Handles all required fields

**Method Breakdown:**

#### `withUsername(String username)`
```java
.withUsername(user.getUsername())
```
- Sets the username
- Used for authentication and display
- Example: "admin"

#### `password(String password)`
```java
.password(user.getPassword())
```
- Sets the encoded password hash
- Should be BCrypt-encoded
- Example: "$2a$10$N9qo8uLOickgx2ZMRZoMye..."

**Important:** 
- Don't encode here (already encoded in DB)
- Password verification handled by `PasswordEncoder`

#### `roles(String... roles)`
```java
.roles(user.getRole().name())
```
- Sets user's roles
- Automatically adds "ROLE_" prefix
- Example: `.roles("ADMIN")` → Authority: "ROLE_ADMIN"

**Role vs Authority:**
| Input | Method | Resulting Authority |
|-------|--------|---------------------|
| "ADMIN" | `.roles("ADMIN")` | "ROLE_ADMIN" |
| "ROLE_ADMIN" | `.authorities("ROLE_ADMIN")` | "ROLE_ADMIN" |

**Why `.roles()` adds prefix:**
```java
// Spring Security convention
.roles("ADMIN")           // Creates "ROLE_ADMIN"
.roles("USER")            // Creates "ROLE_USER"

// Equivalent to:
.authorities("ROLE_ADMIN")
.authorities("ROLE_USER")
```

#### `build()`
```java
.build()
```
- Creates the `UserDetails` object
- Sets default values for optional fields:
  - `accountNonExpired: true`
  - `accountNonLocked: true`
  - `credentialsNonExpired: true`
  - `enabled: true`

---

## Complete Conversion Example

### Input: Database User
```java
User {
    id: 1,
    username: "admin",
    emailId: "admin@bookverse.com",
    password: "$2a$10$N9qo8uLOickgx2ZMRZoMye...",
    role: ADMIN,
    createdAt: 2024-01-15,
    updatedAt: 2024-01-15
}
```

### Output: UserDetails
```java
UserDetails {
    username: "admin",
    password: "$2a$10$N9qo8uLOickgx2ZMRZoMye...",
    authorities: [SimpleGrantedAuthority("ROLE_ADMIN")],
    accountNonExpired: true,
    accountNonLocked: true,
    credentialsNonExpired: true,
    enabled: true
}
```

### What Changed:
- ✅ Username: Copied
- ✅ Password: Copied (hash)
- ✅ Role: Converted to Authority with prefix
- ➕ Account flags: Set to defaults
- ❌ Email, ID, dates: Not included (not needed by Spring Security)

---

## Usage in Authentication Flow

### 1. During Login

```
POST /api/auth/login
Body: {username: "admin", password: "admin123"}
    ↓
AuthService.loginUser()
    ↓
authenticationManager.authenticate()
    ↓
DaoAuthenticationProvider.authenticate()
    ↓
CustomUserDetailsService.loadUserByUsername("admin") ← CALLED HERE
    ↓
Query database for user
    ↓
Return UserDetails with:
    - username: "admin"
    - password: "$2a$10..." (hash)
    - authorities: [ROLE_ADMIN]
    ↓
DaoAuthenticationProvider compares passwords
    ↓
PasswordEncoder.matches("admin123", "$2a$10...")
    ↓
If match: Authentication successful ✅
```

### 2. During JWT Filter Processing

```
GET /api/user/all
Header: Authorization: Bearer eyJhbGc...
    ↓
JwtAuthenticationFilter.doFilterInternal()
    ↓
Extract username from token: "admin"
    ↓
CustomUserDetailsService.loadUserByUsername("admin") ← CALLED HERE
    ↓
Query database for user
    ↓
Return UserDetails
    ↓
Set in SecurityContext
    ↓
Authorization checks use this UserDetails
```

---

## Advanced Customizations

### Adding Account Status Checks

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username);
    if (user == null) {
        throw new UsernameNotFoundException("User not found: " + username);
    }
    
    return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .accountExpired(user.isAccountExpired())
            .accountLocked(user.isAccountLocked())
            .credentialsExpired(user.isPasswordExpired())
            .disabled(!user.isEnabled())
            .build();
}
```

**Additional Methods:**

| Method | Default | Custom Check | Exception if False |
|--------|---------|--------------|-------------------|
| `accountExpired()` | false | Check expiration date | `AccountExpiredException` |
| `accountLocked()` | false | Check lock status | `LockedException` |
| `credentialsExpired()` | false | Check password age | `CredentialsExpiredException` |
| `disabled()` | false | Check active status | `DisabledException` |

### Supporting Multiple Roles

If your User has multiple roles:

```java
@Entity
public class User {
    // ...
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Role> roles;
}
```

```java
@Override
public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username);
    if (user == null) {
        throw new UsernameNotFoundException("User not found: " + username);
    }
    
    String[] rolesArray = user.getRoles().stream()
                             .map(Role::name)
                             .toArray(String[]::new);
    
    return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .roles(rolesArray)  // Multiple roles
            .build();
}
```

### Custom UserDetails Implementation

```java
public class CustomUserDetails implements UserDetails {
    private User user;
    
    public CustomUserDetails(User user) {
        this.user = user;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
    
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    @Override
    public String getUsername() {
        return user.getUsername();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    // Custom methods
    public String getEmail() {
        return user.getEmailId();
    }
    
    public Long getId() {
        return user.getId();
    }
}
```

```java
@Override
public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username);
    if (user == null) {
        throw new UsernameNotFoundException("User not found: " + username);
    }
    return new CustomUserDetails(user);
}
```

---

## Error Handling

### UsernameNotFoundException

```java
throw new UsernameNotFoundException("User not found with username: " + username);
```

**Caught by:** `AuthenticationManager`

**Converted to:** `BadCredentialsException`

**HTTP Response:** 401 Unauthorized

**Example:**
```
User tries to login with username "hacker"
    ↓
CustomUserDetailsService.loadUserByUsername("hacker")
    ↓
userRepository.findByUsername("hacker") → null
    ↓
throw UsernameNotFoundException("User not found...")
    ↓
AuthenticationManager catches
    ↓
Returns BadCredentialsException
    ↓
Client receives: {"error": "Bad credentials"}
```

**Security:**
- Generic error message to client
- Don't reveal whether username exists
- Same response for wrong password

---

## Integration with Other Components

### 1. With AuthenticationManager

```java
// In AuthService:
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(username, password)
);
    ↓
// Internally calls:
CustomUserDetailsService.loadUserByUsername(username)
```

### 2. With JwtAuthenticationFilter

```java
// In JwtAuthenticationFilter:
UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
```

### 3. With PasswordEncoder

```java
// During authentication:
UserDetails userDetails = loadUserByUsername(username);
    ↓
passwordEncoder.matches(rawPassword, userDetails.getPassword())
```

---

## Testing

### Unit Test Example

```java
@Test
public void testLoadUserByUsername_UserExists() {
    // Arrange
    User user = User.builder()
                   .username("admin")
                   .password("$2a$10$...")
                   .role(Role.ADMIN)
                   .build();
    
    when(userRepository.findByUsername("admin")).thenReturn(user);
    
    // Act
    UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin");
    
    // Assert
    assertEquals("admin", userDetails.getUsername());
    assertEquals("$2a$10$...", userDetails.getPassword());
    assertTrue(userDetails.getAuthorities()
              .contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
}

@Test
public void testLoadUserByUsername_UserNotFound() {
    // Arrange
    when(userRepository.findByUsername("nonexistent")).thenReturn(null);
    
    // Act & Assert
    assertThrows(UsernameNotFoundException.class, () -> {
        customUserDetailsService.loadUserByUsername("nonexistent");
    });
}
```

---

## Common Issues & Solutions

### Issue 1: "Bad credentials" despite correct password

**Cause:** Password in database not BCrypt-encoded

**Solution:**
```java
// When creating user:
user.setPassword(passwordEncoder.encode(rawPassword));
userRepository.save(user);
```

### Issue 2: Roles not working

**Cause:** Missing "ROLE_" prefix

**Solution:**
```java
// ✅ Use .roles() (auto-adds prefix)
.roles(user.getRole().name())

// Or manually add prefix:
.authorities("ROLE_" + user.getRole().name())
```

### Issue 3: NullPointerException on user.getRole()

**Cause:** User fetched without role (lazy loading)

**Solution:**
```java
// In User entity:
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Role role = Role.USER; // Default value

// Or in repository:
@EntityGraph(attributePaths = {"role"})
User findByUsername(String username);
```

---

## Best Practices

### ✅ DO:
- Return consistent error for user not found and wrong password
- Use BCrypt-encoded passwords
- Set reasonable account status defaults
- Keep method lightweight (just database lookup)

### ❌ DON'T:
- Don't encode password here (already encoded)
- Don't reveal whether username exists in error message
- Don't load unnecessary data (eager fetch entire object graph)
- Don't perform business logic here

---

## Summary

**CustomUserDetailsService** is the bridge between your database and Spring Security:

1. **Loads** user from database via `UserRepository`
2. **Converts** your `User` entity to Spring Security's `UserDetails`
3. **Handles** user not found errors
4. **Provides** username, password hash, and roles to Spring Security
5. **Enables** authentication and authorization throughout the application

It's a simple but critical component that makes Spring Security work with your custom user model!
