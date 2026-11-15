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
        originalRequest: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // A website request carrying the login cookie is presented to the rest of the app as if it
        // had sent "Authorization: Bearer <token>", so controllers that read that header keep working.
        val cookieToken = if (originalRequest.getHeader("Authorization") == null && WebSessionCookie.isWebRequest(originalRequest))
            WebSessionCookie.read(originalRequest) else null
        val request: HttpServletRequest = if (cookieToken == null) originalRequest else
            object : jakarta.servlet.http.HttpServletRequestWrapper(originalRequest) {
                override fun getHeader(name: String): String? =
                    if (name.equals("Authorization", ignoreCase = true)) "Bearer $cookieToken" else super.getHeader(name)
                override fun getHeaders(name: String): java.util.Enumeration<String> =
                    if (name.equals("Authorization", ignoreCase = true)) java.util.Collections.enumeration(listOf("Bearer $cookieToken"))
                    else super.getHeaders(name)
                override fun getHeaderNames(): java.util.Enumeration<String> {
                    val names = super.getHeaderNames().toList().toMutableList()
                    if (names.none { it.equals("Authorization", ignoreCase = true) }) names.add("Authorization")
                    return java.util.Collections.enumeration(names)
                }
            }
        val authHeader = request.getHeader("Authorization")
        val username: String

        // Android sends "Authorization: Bearer ...". The website sends its HttpOnly cookie, which is
        // only accepted together with the "X-Client: web" header (cross-site requests can't add it).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }
        val jwt = authHeader.substring(7)
        // In our case "username" is the mobile number
        try {
            username = jwtService.extractUsername(jwt)
            val sessionKey = JwtService.sessionKey(username, jwtService.extractClient(jwt))
            val activeToken = redisService.getValue(sessionKey)
            
            // Only proceed if this token matches the one currently active in Redis
            // If activeToken is null (e.g., Redis restarted or key expired early), we trust the JWT if it's still valid
            if ((activeToken == jwt || activeToken == null) && SecurityContextHolder.getContext().authentication == null) {
                if (jwtService.isTokenValid(jwt, username)) {
                    // Restore the session in Redis if it was missing 
                    if (activeToken == null) {
                        redisService.saveValue(sessionKey, jwt, 315360000)
                    }

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
