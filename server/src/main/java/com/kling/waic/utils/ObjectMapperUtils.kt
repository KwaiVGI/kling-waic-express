package com.kling.waic.utils

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.kling.waic.exception.WAICJsonProcessingException
import java.io.IOException


class ObjectMapperUtils {

    companion object {
        var MAPPER: ObjectMapper = ObjectMapper(JsonFactory().disable(JsonFactory.Feature.INTERN_FIELD_NAMES))
            .registerModule(GuavaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .registerModule(ParameterNamesModule())
            .registerModule(JavaTimeModule())

        fun toJSON(obj: Any?): String? {
            if (obj == null) {
                return null
            }
            try {
                return MAPPER.writeValueAsString(obj)
            } catch (e: JsonProcessingException) {
                throw WAICJsonProcessingException(e)
            }
        }

        fun <T> fromJSON(json: String?, valueType: Class<T>): T? {
            if (json == null) {
                return null
            }
            try {
                return MAPPER.readValue<T>(json, valueType)
            } catch (e: IOException) {
                throw WAICJsonProcessingException(e)
            }
        }

        inline fun <reified T> fromJSON(json: String, typeRef: TypeReference<T>): T? {
            return try {
                MAPPER.readValue(json, typeRef)
            } catch (e: Exception) {
                throw WAICJsonProcessingException(e)
            }
        }
    }
}