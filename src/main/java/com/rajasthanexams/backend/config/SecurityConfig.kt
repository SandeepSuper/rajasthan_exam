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
    private val jwtAuthFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
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
