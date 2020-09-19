package com.valensas.common.kafka.webflux.deserializer

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JsonKafkaModelDeserializer(
    private val mappings: Map<String, KClass<*>>,
    private val mapper: ObjectMapper
) : Deserializer<Any> {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) = Unit

    override fun deserialize(topic: String, data: ByteArray?): Any? {
        if (data == null) {
            return null
        }
        if (logger.isDebugEnabled) {
            logger.debug("Deserializing message from topic {}: {}", topic, String(data))
        }

        val targetClass = mappings[topic] ?: mappings.entries.find { (regex, _) -> regex.toRegex().matches(topic) }?.value
        return targetClass?.let { mapper.readValue(data, it.java) }
    }

    override fun close() = Unit
}
