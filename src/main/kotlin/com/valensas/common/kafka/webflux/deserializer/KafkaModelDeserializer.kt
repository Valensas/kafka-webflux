package com.valensas.common.kafka.webflux.deserializer

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class KafkaModelDeserializer(
    private val mapper: ObjectMapper,
    private val mappings: Map<String, KClass<*>>
) : Deserializer<Any> {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) = Unit

    override fun deserialize(topic: String, data: ByteArray?): Any? {
        if (data == null) {
            return null
        }
        logger.debug("Deserializing message from topic {}: {}", topic, String(data))
        return mappings[topic]?.java.let { mapper.readValue(data, it) }
    }

    override fun close() = Unit
}
