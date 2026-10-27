package com.attendease.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // Enable CORS integration with Spring Security
                .cors(Customizer.withDefaults())

                // Disable CSRF for this stateless REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth

                        // Allow browser preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow registration without authentication
                        .requestMatchers(
                                "/api/register",
                                "/api/registrations",
                                "/h2-console/**"
                        ).permitAll()

                        // Require authentication for other endpoints
                        .anyRequest().authenticated()
                )

                // H2 console support
                .headers(headers ->
                        headers.frameOptions(
                                HeadersConfigurer.FrameOptionsConfig::sameOrigin
                        )
                )

                // Retain HTTP Basic authentication
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Your deployed Angular frontend
        configuration.setAllowedOrigins(
                List.of("https://androidpractiseui.netlify.app")
        );

        // HTTP methods permitted by the API
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // Headers permitted in browser requests
        configuration.setAllowedHeaders(
                List.of("Content-Type", "Authorization", "Accept")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

