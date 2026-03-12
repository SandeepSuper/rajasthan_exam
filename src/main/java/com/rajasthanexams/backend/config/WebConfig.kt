package com.rajasthanexams.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebConfig : WebMvcConfigurer {

    @Value("\${file.upload-dir:uploads}")
    private val uploadDir: String = "uploads"

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val path = Paths.get(uploadDir).toAbsolutePath().toUri().toString()
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(path)
    }
}
