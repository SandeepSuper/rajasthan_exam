package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.NewsItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NewsRepository : JpaRepository<NewsItem, UUID> {
    fun findAllByOrderByDateDesc(): List<NewsItem>
}
