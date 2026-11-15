package com.rajasthanexams.backend.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import java.time.Duration

/**
 * The website keeps its login in an HttpOnly cookie instead of browser storage, so page scripts
 * (including any injected script) can never read the token. The Android app keeps using the
 * Authorization header and is not affected.
 *
 * CSRF: the cookie is SameSite=Strict, and the auth filter only accepts it on requests that also
 * carry the "X-Client: web" header, which a cross-site form or link cannot add.
 */
object WebSessionCookie {
    const val NAME = "rex_session"
    private val MAX_AGE: Duration = Duration.ofDays(30)

    fun isWebRequest(request: HttpServletRequest): Boolean =
        request.getHeader("X-Client")?.lowercase() == "web"

    private fun isHttps(request: HttpServletRequest): Boolean =
        request.isSecure || request.getHeader("X-Forwarded-Proto")?.lowercase() == "https"

    fun set(request: HttpServletRequest, response: HttpServletResponse, token: String) {
        response.addHeader("Set-Cookie", build(request, token, MAX_AGE).toString())
    }

    fun clear(request: HttpServletRequest, response: HttpServletResponse) {
        response.addHeader("Set-Cookie", build(request, "", Duration.ZERO).toString())
    }

    fun read(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == NAME }?.value?.takeIf { it.isNotBlank() }

    private fun build(request: HttpServletRequest, value: String, maxAge: Duration) =
        ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(isHttps(request))
            .sameSite("Strict")
            .path("/api")
            .maxAge(maxAge)
            .build()
}
