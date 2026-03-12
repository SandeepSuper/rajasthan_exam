package com.rajasthanexams.backend.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.rajasthanexams.backend.model.Notification
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.CopyOnWriteArrayList

@Component
class NotificationWebSocketHandler : TextWebSocketHandler() {

    private val sessions = CopyOnWriteArrayList<WebSocketSession>()
    private val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
    }

    fun broadcastNotification(notification: Notification) {
        val payload = objectMapper.writeValueAsString(notification)
        val message = TextMessage(payload)
        for (session in sessions) {
            if (session.isOpen) {
                try {
                    session.sendMessage(message)
                } catch (e: Exception) {
                    // Ignore transient network errors on broadcast
                }
            }
        }
    }
}
