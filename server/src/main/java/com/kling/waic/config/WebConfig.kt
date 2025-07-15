package com.kling.waic.config

import com.kling.waic.auth.AuthorizationInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WebConfig(
    private val authorizationInterceptor: AuthorizationInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authorizationInterceptor)
            .addPathPatterns("/**") // Intercept all requests
            .excludePathPatterns(
                "/**/*.html", // Exclude static HTML files
                "/**/*.js", // Exclude static JS files
                "/**/*.css", // Exclude static CSS files
                "/**/*.jpg", // Exclude JPG images
                "/**/*.jpeg", // Exclude JPEG images
                "/**/*.png", // Exclude PNG images
                "/**/*.gif", // Exclude GIF images
            )
    }
}
