package com.rajasthanexams.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    // Comma-separated list of website origins allowed to call the API from a browser
    @org.springframework.beans.factory.annotation.Value("\${app.cors.allowed-origins:http://localhost:5173}")
    private val allowedOrigins: String
) {

    @Bean
    fun corsConfigurationSource(): org.springframework.web.cors.CorsConfigurationSource {
        val config = org.springframework.web.cors.CorsConfiguration()
        config.allowedOrigins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("Authorization", "Content-Type", "X-Client")
        config.allowCredentials = true // the website's login cookie
        config.maxAge = 3600
        val source = org.springframework.web.cors.UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", config)
        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/admin-auth/**").permitAll()
                    .requestMatchers("/api/admin/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .requestMatchers("/api/upload/**").permitAll()
                    .requestMatchers("/import.html").permitAll()
                    .requestMatchers("/admin/**").permitAll()
                    .requestMatchers("/admin-auth.js").permitAll()   // ← static JS for admin pages
                    .requestMatchers("/*.js", "/*.css", "/*.ico", "/*.html", "/static/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/news", "/api/news/**").permitAll() // Current affairs - public read
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/notifications", "/api/notifications/**").permitAll() // Notifications - public read
                    .requestMatchers("/api/ws/notifications", "/api/ws/notifications/**").permitAll() // WebSocket public streams
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/config").permitAll() // App config - public
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/top-referrers").permitAll() // Top referrers - public
                    .anyRequest().authenticated() // Secure everything else
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
