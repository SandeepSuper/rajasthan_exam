package com.rajasthanexams.backend.repository

import com.rajasthanexams.backend.model.AppConfig
import org.springframework.data.jpa.repository.JpaRepository

interface AppConfigRepository : JpaRepository<AppConfig, Long>
