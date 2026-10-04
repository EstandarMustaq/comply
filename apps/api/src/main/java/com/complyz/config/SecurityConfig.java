package com.complyz.config;

import com.complyz.api.ApiError;
import com.complyz.api.ApiErrorEnvelope;
import com.complyz.api.ApiResponseMetadata;
import com.complyz.auth.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String API_BASE_PATH = ApiVersioningConfiguration.API_BASE_PATH;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${complyz.security.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean apiDocsEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                    writeApiError(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Não autorizado: credenciais ausentes ou inválidas"
                    );
                })
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeApiError(response, HttpServletResponse.SC_FORBIDDEN, "Acesso não permitido."))
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(
                                    API_BASE_PATH + "/auth/login",
                                    API_BASE_PATH + "/auth/mfa/verify",
                                    API_BASE_PATH + "/auth/mfa/enroll",
                                    API_BASE_PATH + "/auth/mfa/confirm",
                                    API_BASE_PATH + "/auth/refresh"
                            ).permitAll()
                            .requestMatchers("/actuator/**", "/error").permitAll();
                    if (apiDocsEnabled) {
                        auth.requestMatchers("/v3/api-docs/**").permitAll();
                    }
                    auth.requestMatchers(API_BASE_PATH + "/settings/users/**").hasRole("ADMIN")
                            .requestMatchers(API_BASE_PATH + "/audit-log/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOriginPatterns(origins.isEmpty() ? List.of("*") : origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeApiError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        String requestId = UUID.randomUUID().toString();
        response.setContentType("application/json");
        response.setStatus(status);
        response.setHeader("X-Request-Id", requestId);
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorEnvelope(
                        new ApiError("HTTP_" + status, message, Map.of()),
                        new ApiResponseMetadata(Instant.now(), requestId)
                )
        );
    }
}
