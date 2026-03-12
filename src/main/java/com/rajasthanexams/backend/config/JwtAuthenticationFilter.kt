package com.rajasthanexams.backend.config

import com.rajasthanexams.backend.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        val jwt: String
        val username: String

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        jwt = authHeader.substring(7)
        // In our case "username" is the mobile number
        try {
            username = jwtService.extractUsername(jwt)
            if (SecurityContextHolder.getContext().authentication == null) {
                if (jwtService.isTokenValid(jwt, username)) {
                    val authToken = UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        emptyList() // We can load authorities here if needed
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        } catch (e: Exception) {
            // Token invalid or expired
            println("JWT Validation Failed: ${e.message}")
        }
        filterChain.doFilter(request, response)
    }
}
