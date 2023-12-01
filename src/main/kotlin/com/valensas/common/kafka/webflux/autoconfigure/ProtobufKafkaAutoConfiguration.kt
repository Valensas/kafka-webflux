package com.valensas.common.kafka.webflux.autoconfigure

import com.google.protobuf.Parser
import com.valensas.common.kafka.webflux.consumer.KafkaConsumerDescriptor
import com.valensas.common.kafka.webflux.deserializer.CustomKafkaModelDeserializer
import com.valensas.common.kafka.webflux.serializer.ProtobufSerializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.reflect.full.staticFunctions

@Configuration
@AutoConfigureBefore(KafkaProducerAutoConfiguration::class)
@ConditionalOnProperty(prefix = "spring.kafka", name = ["serialization"], havingValue = "protobuf")
class ProtobufKafkaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.kafka.consumer", name = ["bootstrap-servers"])
    fun protobufDeserializer(consumers: List<KafkaConsumerDescriptor>): Deserializer<*> {
        val topicMappings = mutableMapOf<String, (ByteArray) -> Any>()

        consumers.forEach {
            val existingClass = topicMappings[it.topic]
            if (existingClass != null && existingClass != it.modelType) {
                throw IllegalStateException("Topic ${it.topic} registered with both $existingClass and ${it.modelType}")
            }
            val parserMethod =
                it.modelType.staticFunctions.find {
                    it.name == "parser" && it.parameters.count() == 0
                } ?: throw IllegalStateException("Class ${it.modelType.qualifiedName} does have a parser() static method.")

            val parser = parserMethod.call() as Parser<*>

            topicMappings[it.topic] = parser::parseFrom
        }

        return CustomKafkaModelDeserializer(topicMappings)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.kafka.producer", name = ["bootstrap-servers"])
    fun protobufSerializer(): Serializer<*> = ProtobufSerializer()
}
