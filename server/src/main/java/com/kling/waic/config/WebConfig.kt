package com.kling.waic.config

import com.kling.waic.auth.AuthorizationInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WebConfig (
    private val authorizationInterceptor: AuthorizationInterceptor,
    @Value("\${waic.sudoku.images-dir}")
    private val sudokuImagesDir: String,
    @Value("\${waic.sudoku.url-prefix}")
    private val sudokuUrlPrefix: String
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authorizationInterceptor)
            .addPathPatterns("/**") // Intercept all requests
            .excludePathPatterns(
                "/**/*.html", // Exclude static HTML files
                "/**/*.js", // Exclude static JS files
                "/**/*.css" // Exclude static CSS files
            )
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // make sure endsWith /
        val urlPattern = if (sudokuUrlPrefix.endsWith("/")) {
            "${sudokuUrlPrefix}**"
        } else {
            "${sudokuUrlPrefix}/**"
        }

        val resourceLocation = if (sudokuImagesDir.startsWith("file:")) {
            sudokuImagesDir
        } else {
            "file:${sudokuImagesDir}/"
        }

        registry.addResourceHandler(urlPattern)
            .addResourceLocations(resourceLocation)
            .setCachePeriod(3600) // cache for 1h
    }
}