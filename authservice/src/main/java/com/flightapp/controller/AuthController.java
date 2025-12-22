package com.flightapp.controller;

import com.flightapp.model.User;
import com.flightapp.repo.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer:flightapp}")
    private String issuer;

    @Value("${app.jwt.ttl:3600000}")
    private long ttl;

    @Value("${app.admin.secret:}")
    private String adminSecret;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record SignupRequest(String username, String password, String email, String adminSecret) {}
    public record LoginRequest(String username, String password) {}
    public record TokenResponse(String token) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    /**
     * SECURITY: SignupRequest does NOT include a role parameter.
     * All new users are created with "USER" role by default.
     * Only ADMIN users can be created via database seeding in DataInitializer.
     * This prevents privilege escalation attacks.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        String hashed = passwordEncoder.encode(req.password());
        String role = (req.adminSecret() != null && !req.adminSecret().isBlank() && req.adminSecret().equals(adminSecret)) ? "ADMIN" : "USER";
        User user = new User(null, req.username(), req.email(), hashed, role);
        userRepository.save(user);

        return ResponseEntity.ok("User created");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByUsername(req.username());
        if (userOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid username or password");

        User user = userOpt.get();

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash()))
            return ResponseEntity.status(401).body("Invalid username or password");

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();

        String jwt = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttl))
                .claim("roles", user.getRoles())     // stored as string → fine
                .signWith(key)
                .compact();

        return ResponseEntity.ok(new TokenResponse(jwt));
    }

    private ResponseEntity<?> handlePasswordChangeForUser(String username, ChangePasswordRequest req) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(400).body("Current password is incorrect");
        }

        if (req.newPassword() == null || req.newPassword().length() < 8) {
            return ResponseEntity.badRequest().body("New password must be at least 8 characters");
        }

        String newHash = passwordEncoder.encode(req.newPassword());
        user.setPasswordHash(newHash);
        userRepository.save(user);
        return ResponseEntity.ok("Password changed successfully");
    }

    /**
     * Change password using JWT Authorization header (existing behavior)
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody ChangePasswordRequest req
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }

        String token = authorization.substring("Bearer ".length());
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String username;
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            username = claims.getSubject();
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token");
        }

        return handlePasswordChangeForUser(username, req);
    }

    /**
     * Change password using X-User-Name header (assignment requirement)
     */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePasswordPut(
            @RequestHeader(name = "X-User-Name") String username,
            @RequestBody ChangePasswordRequest req
    ) {
        return handlePasswordChangeForUser(username, req);
    }
}
