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
import java.net.InetSocketAddress
import java.net.Proxy

@Configuration
open class ServiceConfig(
    @param:Value("\${jedis.host}") private val jedisHost: String,
    @param:Value("\${jedis.port}") private val jedisPort: Int,
    @param:Value("\${jedis.password}") private val jedisPassword: String,
    @param:Value("\${kling.proxy.host}") private val proxyHost: String,
    @param:Value("\${kling.proxy.port}") private val proxyPort: Int
) {

    @Bean
    open fun jedis(): Jedis {
        val jedis = Jedis(jedisHost, jedisPort)
        jedis.auth(jedisPassword)
        return jedis
    }

    @Bean
    open fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(proxyHost, proxyPort)
                )
            )
            .build()
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
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        val inputStream =
            this::class.java.classLoader.getResourceAsStream("haarcascade_frontalface_alt.xml")
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