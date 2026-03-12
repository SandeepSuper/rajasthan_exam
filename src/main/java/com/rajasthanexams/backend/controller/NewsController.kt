package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.model.NewsItem
import com.rajasthanexams.backend.repository.NewsRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/news")
@Tag(name = "Current Affairs", description = "Daily updates and news")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
class NewsController(
    private val newsRepository: NewsRepository
) {

    @GetMapping
    @Operation(summary = "Get News", description = "List news items ordered by date.")
    fun getNews(): ResponseEntity<List<NewsItem>> {
        val news = newsRepository.findAllByOrderByDateDesc()
        return ResponseEntity.ok(news)
    }

    // Admin only - simplified
    @PostMapping
    @Operation(summary = "Create News", description = "Add a new current affairs item.")
    fun createNews(@RequestBody newsItem: NewsItem): ResponseEntity<NewsItem> {
        val saved = newsRepository.save(newsItem)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete News", description = "Delete a news item by ID.")
    fun deleteNews(@PathVariable id: UUID): ResponseEntity<Void> {
        return if (newsRepository.existsById(id)) {
            newsRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk Import News", description = "Import multiple current affairs items at once.")
    fun bulkImport(@RequestBody items: List<NewsItem>): ResponseEntity<List<NewsItem>> {
        val saved = newsRepository.saveAll(items)
        return ResponseEntity.ok(saved)
    }
}
