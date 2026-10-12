package com.arohan.strengthclub.config;

import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.security.AccountStatusService;
import com.arohan.strengthclub.security.RevokedTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Configures stateless JWT authentication, role boundaries, JSON security errors, and CORS. */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        // Cost 10 is the OWASP-accepted minimum and keeps login/register around 4x faster than cost 12.
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
            JsonMapper objectMapper) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "error", "UNAUTHENTICATED",
                    "message", "Please sign in to continue."));
        };
        AccessDeniedHandler deniedHandler = (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "error", "FORBIDDEN",
                    "message", "You do not have permission to perform this action."));
        };
        http
                // The API authenticates with bearer tokens rather than browser-managed sessions.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/health",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api/gym/status",
                                "/api/membership-plans",
                                "/api/auth/register",
                                "/api/auth/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/trainer/**").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers("/api/member/**").hasRole("MEMBER")
                        .requestMatchers("/api/auth/me").authenticated()
                        // Fail closed: new API routes must be explicitly permitted or authenticated.
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Expired/invalid tokens must also answer with the JSON shape the frontend understands.
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler));
        return http.build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(AccountStatusService accounts, RevokedTokenService revokedTokens) {
        return jwt -> {
            if (revokedTokens.isRevoked(jwt.getId())) {
                throw new BadCredentialsException("Token was signed out.");
            }
            String role = jwt.getClaimAsString("role");
            String uid = jwt.getClaimAsString("uid");
            if (uid != null && !uid.isBlank()) {
                UUID userId;
                try {
                    userId = UUID.fromString(uid);
                } catch (IllegalArgumentException ex) {
                    throw new BadCredentialsException("Invalid token.");
                }
                // The live role wins over the token claim: demotions and deletions apply immediately.
                Role live = accounts.currentRole(userId)
                        .orElseThrow(() -> new BadCredentialsException("Account no longer exists."));
                role = live.name();
            }
            List<SimpleGrantedAuthority> authorities = role == null || role.isBlank()
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(java.util.Locale.ROOT)));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tab-Session-Id"));
        config.setExposedHeaders(List.of("Location"));
        // Authentication uses the Authorization header; browser cookies are not required.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}