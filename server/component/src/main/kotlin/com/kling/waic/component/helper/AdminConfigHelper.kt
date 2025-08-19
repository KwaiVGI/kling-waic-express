package com.kling.waic.component.helper

import com.kling.waic.component.config.AdminConfig
import com.kling.waic.component.utils.ObjectMapperUtils
import org.springframework.stereotype.Component
import redis.clients.jedis.commands.JedisCommands

@Component
class AdminConfigHelper(
    private val jedis: JedisCommands
) {

    fun getAdminConfig(): AdminConfig {
        val adminConfigValue = jedis.get("adminConfig")
        if (adminConfigValue.isNullOrEmpty()) {
            return AdminConfig()
        }
        return ObjectMapperUtils.fromJSON(adminConfigValue, AdminConfig::class.java)!!

    }

    fun saveAdminConfig(adminConfig: AdminConfig): String {
        val adminConfigValue = ObjectMapperUtils.toJSON(adminConfig)
        return jedis.set("adminConfig", adminConfigValue)
    }
}