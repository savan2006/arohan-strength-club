package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.AuthDtos.AuthResponse;
import com.arohan.strengthclub.dto.AuthDtos.LoginRequest;
import com.arohan.strengthclub.dto.AuthDtos.RegisterRequest;
import com.arohan.strengthclub.dto.AuthDtos.UserResponse;
import com.arohan.strengthclub.security.CurrentUserService;
import com.arohan.strengthclub.security.RevokedTokenService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.arohan.strengthclub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public registration/sign-in endpoints and the authenticated user's profile summary. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUsers;
    private final RevokedTokenService revokedTokens;

    public AuthController(AuthService authService, CurrentUserService currentUsers,
                          RevokedTokenService revokedTokens) {
        this.authService = authService;
        this.currentUsers = currentUsers;
        this.revokedTokens = revokedTokens;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Invalidates the presented token so a copied/stolen token stops working after sign-out. */
    @PostMapping("/logout")
    public java.util.Map<String, String> logout(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            revokedTokens.revoke(jwtAuth.getToken().getId(), jwtAuth.getToken().getExpiresAt());
        }
        return java.util.Map.of("message", "Signed out.");
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(currentUsers.requireUser(authentication));
    }
}
