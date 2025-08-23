package com.kling.waic.api.config

import com.kling.waic.component.handler.ActivityHandler
import com.kling.waic.component.utils.FileUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.opencv.core.Core
import org.opencv.objdetect.CascadeClassifier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.*
import redis.clients.jedis.commands.JedisCommands
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.io.File
import java.io.FileOutputStream

@Configuration
open class ServiceConfig(
    @param:Value("\${REDIS_CLUSTER_MODE_WAIC:false}") private val jedisClusterMode: Boolean,
    @param:Value("\${REDIS_HOST_WAIC:}") private val jedisHost: String,
    @param:Value("\${REDIS_PORT_WAIC:}") private val jedisPort: Int,
    @param:Value("\${REDIS_PASS_WAIC:}") private val jedisPassword: String,
    @param:Value("\${S3_PROFILE_NAME:}") private val s3ProfileName: String
) {

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Bean
    open fun jedis(): JedisCommands {

        val jedisCommands = if (jedisClusterMode) {
            createJedisCluster()
        } else {
            createStandaloneJedis()
        }
        return jedisCommands
    }

    private fun createJedisCluster(): JedisCluster {
        val maxAttempts = 5

        val clientConfig: JedisClientConfig? = DefaultJedisClientConfig.builder()
            .ssl(true)
            .build()

        val jedisCluster = JedisCluster(
            HostAndPort(jedisHost, jedisPort),
            clientConfig,
            maxAttempts,
            ConnectionPoolConfig()
        )
        return jedisCluster
    }

    private fun createStandaloneJedis(): Jedis {
        val jedis = Jedis(jedisHost, jedisPort)
        jedis.auth(jedisPassword)
        return jedis
    }

    @Bean
    open fun styleImagePrompts(): List<String> {
        return FileUtils.readTextFromResources("style-image-prompts.txt")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    @Bean
    open fun videoSpecialEffects(): List<String> {
        return FileUtils.readTextFromResources("video-special-effects.txt")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    @Bean
    open fun nextCodeLuaScript(): String {
        return FileUtils.readTextFromResources("next-code.lua")
    }

    @Bean
    @ConditionalOnProperty(
        name = ["WAIC_CROP_IMAGE_WITH_OPENCV"],
        havingValue = "true",
        matchIfMissing = true
    )
    open fun loadCascadeClassifiersFromResources(): List<CascadeClassifier> {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        
        // Try to load multiple classifiers in order of priority
        val classifierFiles = listOf(
            "haarcascade_frontalface_default.xml",
            "haarcascade_profileface.xml",
            "haarcascade_frontalface_alt.xml",
            "haarcascade_frontalface_alt2.xml",
        )
        
        val classifiers = mutableListOf<CascadeClassifier>()
        
        for (filename in classifierFiles) {
            try {
                val inputStream = this::class.java.classLoader.getResourceAsStream(filename)
                if (inputStream != null) {
                    val tempFile = File.createTempFile("tmp_${filename.replace(".xml", "")}", ".xml")
                    tempFile.deleteOnExit()
                    FileOutputStream(tempFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    val classifier = CascadeClassifier(tempFile.absolutePath)
                    if (!classifier.empty()) {
                        classifiers.add(classifier)
                        log.info("Successfully loaded cascade classifier: $filename")
                    } else {
                        log.warn("Loaded cascade classifier is empty: $filename")
                    }
                } else {
                    log.debug("Cascade classifier not found in resources: $filename")
                }
            } catch (e: Exception) {
                log.warn("Failed to load cascade classifier: $filename", e)
            }
        }
        
        if (classifiers.isEmpty()) {
            throw IllegalStateException("No cascade classifiers could be loaded from resources")
        }
        
        log.info("Loaded ${classifiers.size} cascade classifiers successfully")
        return classifiers
    }

    @Bean
    open fun awsCredentialsProvider(): AwsCredentialsProvider {
        val credentialsProviderBuilder = DefaultCredentialsProvider.builder()
        if (s3ProfileName.isNotEmpty()) {
            credentialsProviderBuilder.profileName(s3ProfileName)
        }
        return credentialsProviderBuilder.build()
    }

    @Bean
    open fun s3Client(awsCredentialsProvider: AwsCredentialsProvider): S3Client {
        return S3Client.builder()
            .region(Region.CN_NORTH_1)
            .credentialsProvider(awsCredentialsProvider)
            .build()
    }

    @Bean
    fun activityHandlerMap(): Map<String, ActivityHandler> {
        val activityHandlers = applicationContext.getBeansOfType(ActivityHandler::class.java)
        val map = mutableMapOf<String, ActivityHandler>()
        activityHandlers.values.forEach { activityHandler ->
            map[activityHandler.activityName()] = activityHandler
        }
        return map
    }
}