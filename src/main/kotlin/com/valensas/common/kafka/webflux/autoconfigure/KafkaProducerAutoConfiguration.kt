package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions

@Configuration
@ConditionalOnProperty(prefix = "spring.kafka.producer", name = ["bootstrap-servers"])
class KafkaProducerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun kafkaSender(
        kafkaProperties: KafkaProperties,
        serializer: Serializer<Any>
    ): KafkaSender<String, Any> {
        val properties = SenderOptions
            .create<String, Any>(kafkaProperties.buildProducerProperties())
            .withValueSerializer(serializer)

        return KafkaSender.create(properties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun kafkaProducer(sender: KafkaSender<String, *>) = KafkaProducer(sender)
}
