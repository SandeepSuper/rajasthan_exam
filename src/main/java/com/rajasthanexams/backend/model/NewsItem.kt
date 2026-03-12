package com.rajasthanexams.backend.model

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "news_items")
data class NewsItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "title_hi", nullable = false)
    val titleHi: String,

    @Column(name = "title_en")
    val titleEn: String? = null,

    @Column(name = "desc_hi")
    val descHi: String? = null,

    @Column(name = "desc_en")
    val descEn: String? = null,

    @Column(columnDefinition = "DATE")
    val date: LocalDate = LocalDate.now(),

    @Column(name = "image_url")
    val imageUrl: String? = null
)
