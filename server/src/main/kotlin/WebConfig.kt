package com.klingai.express

import com.klingai.express.controllers.AuthorizationInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WebConfig @Autowired constructor(
    val authorizationInterceptor: AuthorizationInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(this.authorizationInterceptor)
            .addPathPatterns("/**") // Intercept all requests
            .excludePathPatterns(
                "/**/*.html", // Exclude static HTML files
                "/**/*.js", // Exclude static JS files
                "/**/*.css" // Exclude static CSS files
            )
    }
}