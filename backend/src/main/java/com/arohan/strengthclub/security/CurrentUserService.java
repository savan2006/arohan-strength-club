package com.arohan.strengthclub.security;

import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Resolves the signed-in account for a request. */
@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) { this.userRepository = userRepository; }

    /** Returns the account id straight from the verified token when possible (no database round trip). */
    public UUID requireUserId(Authentication authentication) {
        UUID fromToken = uidClaim(authentication);
        return fromToken != null ? fromToken : requireByEmail(authentication).getId();
    }

    public User requireUser(Authentication authentication) {
        UUID uid = uidClaim(authentication);
        if (uid != null) {
            return userRepository.findById(uid).orElseThrow(CurrentUserService::accountMissing);
        }
        return requireByEmail(authentication);
    }

    private User requireByEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw accountMissing();
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(CurrentUserService::accountMissing);
    }

    private static UUID uidClaim(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String uid = jwtAuth.getToken().getClaimAsString("uid");
            if (uid != null && !uid.isBlank()) {
                try {
                    return UUID.fromString(uid);
                } catch (IllegalArgumentException ignored) {
                    // fall back to the email subject
                }
            }
        }
        return null;
    }

    /** A valid token for a deleted account must force a fresh sign-in, so answer 401 (not 404). */
    private static BusinessRuleException accountMissing() {
        return new BusinessRuleException("UNAUTHENTICATED",
                "Your account is no longer available. Please sign in again.", HttpStatus.UNAUTHORIZED);
    }
}
