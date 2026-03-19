package com.rajasthanexams.backend.config

import com.rajasthanexams.backend.service.JwtService
import com.rajasthanexams.backend.service.RedisService
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
    private val jwtService: JwtService,
    private val redisService: RedisService
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
            val activeToken = redisService.getValue("session:$username")
            
            // Only proceed if this token matches the one currently active in Redis
            if (activeToken == jwt && SecurityContextHolder.getContext().authentication == null) {
                if (jwtService.isTokenValid(jwt, username)) {
                    val authToken = UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        emptyList() // We can load authorities here if needed
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            } else if (activeToken != jwt) {
                println("JWT Validation Failed: Token does not match active session for $username")
            }
        } catch (e: Exception) {
            // Token invalid or expired
            println("JWT Validation Failed: ${e.message}")
        }
        filterChain.doFilter(request, response)
    }
}
