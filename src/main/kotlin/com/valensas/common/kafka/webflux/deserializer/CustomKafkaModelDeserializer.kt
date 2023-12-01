package com.valensas.common.kafka.webflux.deserializer

import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CustomKafkaModelDeserializer(
    private val mappings: Map<String, (ByteArray) -> Any>
) : Deserializer<Any> {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun configure(
        configs: MutableMap<String, *>?,
        isKey: Boolean
    ) = Unit

    override fun deserialize(
        topic: String,
        data: ByteArray?
    ): Any? {
        if (data == null) {
            return null
        }
        if (logger.isDebugEnabled) {
            logger.debug("Deserializing message from topic {}: {}", topic, String(data))
        }

        val mapper = mappings[topic] ?: mappings.entries.find { (pattern, _) -> pattern.toRegex().matches(topic) }?.value
        return mapper?.invoke(data)
    }

    override fun close() = Unit
}
