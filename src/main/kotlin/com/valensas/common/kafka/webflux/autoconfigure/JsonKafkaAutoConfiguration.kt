package com.valensas.common.kafka.webflux.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import com.valensas.common.kafka.webflux.consumer.KafkaConsumerDescriptor
import com.valensas.common.kafka.webflux.deserializer.JsonKafkaModelDeserializer
import kotlin.reflect.KClass
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@AutoConfigureBefore(KafkaProducerAutoConfiguration::class)
@ConditionalOnProperty(prefix = "spring.kafka", name = ["serialization"], havingValue = "json", matchIfMissing = true)
class JsonKafkaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.kafka.consumer", name = ["bootstrap-servers"])
    fun jsonDeserializer(
        consumers: List<KafkaConsumerDescriptor>,
        objectMapper: ObjectMapper
    ): Deserializer<*> {
        val topicMappings = mutableMapOf<String, KClass<*>>()

        consumers.forEach {
            val existingClass = topicMappings[it.topic]
            if (existingClass != null && existingClass != it.modelType) {
                throw IllegalStateException("Topic ${it.topic} registered with both $existingClass and ${it.modelType}")
            }
            topicMappings[it.topic] = it.modelType
        }

        return JsonKafkaModelDeserializer(topicMappings, objectMapper)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.kafka.producer", name = ["bootstrap-servers"])
    fun jsonSerializer() = JsonSerializer<Any>()
}
