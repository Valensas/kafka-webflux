package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.producer.KafkaProducer
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.boot.kafka.autoconfigure.SslBundleSslEngineFactory
import org.springframework.boot.ssl.SslBundle
import org.springframework.boot.ssl.SslBundles
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
        serializer: Serializer<Any>,
        sslBundles: SslBundles
    ): KafkaSender<String, Any> {
        val producerProps = kafkaProperties.buildProducerProperties()
        applySslBundle(producerProps, kafkaProperties.ssl.bundle, sslBundles)

        val properties =
            SenderOptions
                .create<String, Any>(producerProps)
                .withValueSerializer(serializer)

        return KafkaSender.create(properties)
    }

    private fun applySslBundle(
        properties: MutableMap<String, Any>,
        bundleName: String?,
        sslBundles: SslBundles
    ) {
        if (bundleName.isNullOrBlank()) {
            return
        }
        val bundle = sslBundles.getBundle(bundleName)
        properties[SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG] = SslBundleSslEngineFactory::class.java
        properties[SslBundle::class.java.name] = bundle
    }

    @Bean
    @ConditionalOnMissingBean
    fun kafkaProducer(sender: KafkaSender<String, *>) = KafkaProducer(sender)
}
