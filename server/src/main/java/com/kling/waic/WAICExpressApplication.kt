package com.kling.waic

//import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
open class WAICExpressApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            // load .env
//            val dotenv = Dotenv.configure()
//                .filename(".env")
//                .ignoreIfMissing()
//                .load()
//            dotenv.entries().forEach {
//                System.setProperty(it.key, it.value)
//            }

            runApplication<WAICExpressApplication>(*args)
        }
    }
}