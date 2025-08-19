package com.kling.waic.component.helper

import com.kling.waic.component.entity.AdminConfig
import com.kling.waic.component.utils.ObjectMapperUtils
import org.springframework.stereotype.Component
import redis.clients.jedis.commands.JedisCommands

@Component
class AdminConfigHelper(
    private val jedis: JedisCommands
) {

    companion object {
        private const val ADMIN_CONFIG = "admin_config"
    }

    fun getAdminConfig(): AdminConfig {
        val adminConfigValue = jedis.get(ADMIN_CONFIG)
        if (adminConfigValue.isNullOrEmpty()) {
            return AdminConfig()
        }
        return ObjectMapperUtils.fromJSON(adminConfigValue, AdminConfig::class.java)!!

    }

    fun saveAdminConfig(adminConfig: AdminConfig): String {
        val adminConfigValue = ObjectMapperUtils.toJSON(adminConfig)
        return jedis.set(ADMIN_CONFIG, adminConfigValue)
    }
}