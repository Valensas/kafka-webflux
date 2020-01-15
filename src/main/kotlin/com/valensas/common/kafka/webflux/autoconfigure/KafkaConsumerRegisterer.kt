package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.consumer.KafkaConsumerDescriptor
import com.valensas.common.kafka.webflux.deserializer.KafkaModelDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import javax.annotation.PostConstruct

@Configuration
class KafkaConsumerRegisterer(
    private val kafkaProperties: KafkaProperties,
    private val consumers: List<KafkaConsumerDescriptor>,
    private val kafkaModelDeserializer: KafkaModelDeserializer
) {
    @PostConstruct
    private fun registerConsumers() {
        val consumerProps = kafkaProperties.buildConsumerProperties()

        consumers.forEach { consumer ->
            val receiverOptions = ReceiverOptions
                .create<String, Any>(consumerProps)
                .withValueDeserializer(kafkaModelDeserializer)
                .subscription(listOf(consumer.topic))

            stream(receiverOptions)
                .concatMap { record ->
                    consumer
                        .invoke(record)
                        .map { record }
                }
                .doOnNext {
                    it.receiverOffset().commit()
                }
                .subscribe()
        }
    }

    private fun stream(options: ReceiverOptions<String, Any>): Flux<ReceiverRecord<String, Any>> =
        KafkaReceiver
            .create(options)
            .receive()
            .onErrorResume {
                stream(options)
            }
}
