package com.valensas.common.kafka.webflux.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import com.valensas.common.kafka.webflux.consumer.KafkaConsumerDescriptor
import com.valensas.common.kafka.webflux.deserializer.KafkaModelDeserializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.reflect.KClass

@Configuration
@ConditionalOnProperty(prefix = "spring.kafka.consumer", name = ["bootstrap-servers"])
class KafkaDeserializerAutoConfiguration(
    private val consumers: List<KafkaConsumerDescriptor>,
    private val objectMapper: ObjectMapper
) {

    @Bean
    @ConditionalOnMissingBean
    fun kafkaDeserializer(): KafkaModelDeserializer {
        val topicMappings = mutableMapOf<String, KClass<*>>()

        consumers.forEach {
            val existingClass = topicMappings[it.topic]
            if (existingClass != null && existingClass != it.modelType) {
                throw IllegalStateException("Topic ${it.topic} registered with both $existingClass and ${it.modelType}")
            }
            topicMappings[it.topic] = it.modelType
        }

        return KafkaModelDeserializer(
            objectMapper,
            topicMappings
        )
    }
}
