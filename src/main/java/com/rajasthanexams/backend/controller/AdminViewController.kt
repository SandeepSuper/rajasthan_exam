package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.repository.QuestionReportRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Serves admin HTML pages (Thymeleaf templates).
 * All API calls from these pages are authenticated via JWT in the browser.
 */
@Controller
@RequestMapping("/admin")
class AdminViewController(
    private val reportRepository: QuestionReportRepository
) {

    @GetMapping("/login")
    fun loginPage(): String = "admin/login"

    @GetMapping("/tests")
    fun createTestPage(): String = "admin/create_test"

    @GetMapping("/payments")
    fun paymentsPage(): String = "admin/payments"

    @GetMapping("/content")
    fun contentDashboardPage(): String = "admin/content_dashboard"

    @GetMapping("/reports")
    fun reportsPage(model: Model): String {
        val reports = reportRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
        model.addAttribute("reports", reports)
        return "admin/reports"
    }

    @GetMapping("/news")
    fun newsPage(): String = "admin/news"

    @GetMapping("/notifications")
    fun notificationsPage(): String = "admin/notifications"

    @GetMapping("/community")
    fun communityPage(): String = "admin/community"
}

