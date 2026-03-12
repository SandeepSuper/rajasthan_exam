package com.rajasthanexams.backend.controller

import com.rajasthanexams.backend.dto.ApiResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@RestController
@RequestMapping("/api/upload")
class FileUploadController {

    @Value("\${file.upload-dir:uploads}")
    private val uploadDir: String = "uploads"

    @PostMapping
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("error" to "File is empty"))
        }

        try {
            val directory = File(uploadDir)
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val originalFilename = file.originalFilename ?: "unknown.jpg"
            val extension = originalFilename.substringAfterLast('.', "jpg")
            val newFilename = "${UUID.randomUUID()}.$extension"
            val filePath = Paths.get(uploadDir, newFilename)

            Files.copy(file.inputStream, filePath)

            // Return full URL
            val fileUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(newFilename)
                .toUriString()

            return ResponseEntity.ok(mapOf("url" to fileUrl))
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.internalServerError().body(mapOf("error" to "Upload failed: ${e.message}"))
        }
    }
}
