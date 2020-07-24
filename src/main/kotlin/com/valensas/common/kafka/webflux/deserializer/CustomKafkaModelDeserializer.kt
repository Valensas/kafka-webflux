package com.valensas.common.kafka.webflux.deserializer

import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CustomKafkaModelDeserializer(
    private val mappings: Map<String, (ByteArray) -> Any>
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

        return mappings[topic]?.invoke(data)
    }

    override fun close() = Unit
}
