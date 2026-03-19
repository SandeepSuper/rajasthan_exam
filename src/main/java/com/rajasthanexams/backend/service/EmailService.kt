package com.rajasthanexams.backend.service

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    @Value("\${app.mail.from:noreply@yourdomain.com}")
    private lateinit var fromEmail: String

    fun sendOtpEmail(toEmail: String, otp: String) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setFrom(fromEmail)
        helper.setTo(toEmail)
        helper.setSubject("Your Rajasthan Exams OTP Code")
        helper.setText(
            """
            <html>
              <body style="font-family: Arial, sans-serif; background: #f5f5f5; padding: 24px;">
                <div style="max-width: 480px; margin: auto; background: white; border-radius: 12px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                  <h2 style="color: #3F5CDB; margin-top: 0;">Rajasthan Exams</h2>
                  <p style="color: #333; font-size: 16px;">Use the OTP below to verify your email address:</p>
                  <div style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #3F5CDB; text-align: center; padding: 20px 0;">$otp</div>
                  <p style="color: #666; font-size: 13px;">This OTP is valid for <strong>10 minutes</strong>. Do not share it with anyone.</p>
                  <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;"/>
                  <p style="color: #999; font-size: 12px;">If you did not request this, please ignore this email.</p>
                </div>
              </body>
            </html>
            """.trimIndent(),
            true
        )

        mailSender.send(message)
    }
}
