package com.kling.waic.api.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

//@RestController
@RequestMapping("/sudoku-images")
class SudokuImageController (
    @Value("\${waic.sudoku.images-dir}")
    private val sudokuImagesDir: String,
){

    @GetMapping("/{filename}")
    fun getImage(@PathVariable filename: String): ResponseEntity<Resource> {
        val file = File(sudokuImagesDir, filename)
        
        if (!file.exists() || !file.isFile) {
            return ResponseEntity.notFound().build()
        }

        val resource = FileSystemResource(file)
        val contentType = Files.probeContentType(Paths.get(file.absolutePath))
            ?: MediaType.APPLICATION_OCTET_STREAM_VALUE

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(resource)
    }
}
