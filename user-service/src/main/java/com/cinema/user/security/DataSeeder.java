package com.cinema.user.security;

import com.cinema.user.model.Role;
import com.cinema.user.model.User;
import com.cinema.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Optional local bootstrap: opt in and provide a non-default secret. */
@Component
@ConditionalOnProperty(name = "cinema.bootstrap-admin.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${cinema.bootstrap-admin.username:admin}") private String username;
    @Value("${cinema.bootstrap-admin.password:}") private String password;

    public DataSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }
    @Override
    public void run(String... args) {
        if (password == null || password.length() < 12 || "123456".equals(password)) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD must be at least 12 characters");
        }
        if (users.findByUsername(username).isEmpty()) {
            users.save(new User(username, encoder.encode(password), Role.ADMIN));
        }
    }
}
