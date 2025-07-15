package com.kling.waic.config

import com.kling.waic.utils.FileUtils
import okhttp3.OkHttpClient
import org.opencv.core.Core
import org.opencv.objdetect.CascadeClassifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.Jedis
import java.io.File
import java.io.FileOutputStream

@Configuration
open class ServiceConfig(
    @param:Value("\${jedis.host}") private val host: String,
    @param:Value("\${jedis.port}") private val port: Int,
) {

    @Bean
    open fun jedis(): Jedis {
        val password = System.getenv("REDIS_PASS_WAIC")
        val jedis = Jedis(host, port)
        jedis.auth(password)
        return jedis
    }

    @Bean
    open fun waicManagementToken(jedis: Jedis): String {
        return jedis.get("waic.management.token")
    }

    @Bean
    open fun waicOpenApiAccessKey(jedis: Jedis): String {
        return jedis.get("waic.open-api.access-key")
    }

    @Bean
    open fun waicOpenApiSecretKey(jedis: Jedis): String {
        return jedis.get("waic.open-api.secret-key")
    }

    @Bean
    open fun okHttpClient(): OkHttpClient {
        return OkHttpClient()
    }

    @Bean
    open fun styleImagePrompts(): List<String> {
        return FileUtils.readTextFromResources("style-image-prompts.txt")
            .split("\n")
            .map { it.trim() }
            .toList()
    }

    @Bean
    open fun videoSpecialEffects(): List<String> {
        return FileUtils.readTextFromResources("video-special-effects.txt")
            .split("\n")
            .map { it.trim() }
            .toList()
    }

    @Bean
    open fun nextCodeLuaScript(): String {
        return FileUtils.readTextFromResources("next-code.lua")
    }

    @Bean
    open fun loadCascadeClassifierFromResources(): CascadeClassifier {
//        val opencvPath = System.getenv("OPENCV_PATH")
//        System.load(opencvPath)

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        val inputStream = this::class.java.classLoader.getResourceAsStream("haarcascade_frontalface_alt.xml")
            ?: throw IllegalArgumentException("Cannot find haarcascade XML in resources")

        // copy to a temporary file
        val tempFile = File.createTempFile("tmp_haarcascade_frontalface_alt", ".xml")
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }

        return CascadeClassifier(tempFile.absolutePath)
    }
}