package com.kling.waic.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication(
    scanBasePackages = ["com.kling.waic"]
)
@EnableAspectJAutoProxy(proxyTargetClass = true)
open class WAICExpressApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WAICExpressApplication>(*args)
        }
    }
}