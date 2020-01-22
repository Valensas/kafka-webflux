package com.valensas.common.kafka.webflux.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import com.valensas.common.kafka.webflux.producer.KafkaProducer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonSerializer
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions

@Configuration
class KafkaProducerAutoConfiguration {
    @Bean
    fun kafkaSender(
        kafkaProperties: KafkaProperties,
        mapper: ObjectMapper
    ): KafkaSender<String, Any> {
        val properties = SenderOptions
            .create<String, Any>(kafkaProperties.buildProducerProperties())
            .withValueSerializer(JsonSerializer(mapper))

        return KafkaSender.create<String, Any>(properties)
    }

    @Bean
    fun kafkaProducer(sender: KafkaSender<String, *>) = KafkaProducer(sender)
}
