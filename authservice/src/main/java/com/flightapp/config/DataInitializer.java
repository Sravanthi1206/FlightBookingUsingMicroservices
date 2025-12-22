package com.flightapp.config;

import com.flightapp.model.User;
import com.flightapp.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed admin users on application startup
        seedAdminUsers();
    }

    private void seedAdminUsers() {
        // Create default admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@flightapp.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRoles("ADMIN");
            userRepository.save(admin);
            System.out.println("✓ Admin user created: admin / admin123");
        }

        // Create another admin user
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setEmail("superadmin@flightapp.com");
            superAdmin.setPasswordHash(passwordEncoder.encode("superadmin123"));
            superAdmin.setRoles("ADMIN");
            userRepository.save(superAdmin);
            System.out.println("✓ Admin user created: superadmin / superadmin123");
        }

        // Create a regular user for testing
        if (userRepository.findByUsername("testuser").isEmpty()) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("testuser@flightapp.com");
            testUser.setPasswordHash(passwordEncoder.encode("testuser123"));
            testUser.setRoles("USER");
            userRepository.save(testUser);
            System.out.println("✓ Regular user created: testuser / testuser123");
        }
    }
}
