package com.flightapp.controller;

import com.flightapp.model.User;
import com.flightapp.repo.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record SignupRequest(String username, String password) {}
    public record LoginRequest(String username, String password) {}
    public record TokenResponse(String token) {}

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        String hashed = passwordEncoder.encode(req.password());
        User user = new User(null, req.username(), hashed, "USER");
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
}
