package com.myfinance.track.auth;

import com.myfinance.track.security.JwtService;
import com.myfinance.track.user.User;
import com.myfinance.track.user.UserDto;
import com.myfinance.track.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean registrationEnabled;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.registration-enabled:false}") boolean registrationEnabled) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.registrationEnabled = registrationEnabled;
    }

    @Transactional
    public UserDto register(RegisterRequest req) {
        if (!registrationEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is disabled");
        }
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        try {
            User saved = users.saveAndFlush(
                    new User(email, passwordEncoder.encode(req.password()), req.displayName()));
            return UserDto.from(saved);
        } catch (DataIntegrityViolationException e) {   // two sign-ups racing for the same email
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email())).orElse(null);
        boolean ok = user != null && passwordEncoder.matches(req.password(), user.getPasswordHash());
        if (!ok) {
            // Same message for "no such email" and "wrong password", so emails can't be probed.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return users.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}