package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.consumer.KafkaConsumerDescriptor
import com.valensas.common.kafka.webflux.deserializer.KafkaModelDeserializer
import com.valensas.common.kafka.webflux.util.ReceiverCustomizer
import javax.annotation.PostConstruct
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kotlin.core.publisher.toFlux

private fun <T, R> Flux<T>.flatMapSequential(concurrency: Int?, mapper: (T) -> Publisher<R>): Flux<R> =
    if (concurrency == null)
        flatMapSequential(mapper)
    else
        flatMapSequential(mapper, concurrency)

@Configuration
@ConditionalOnProperty(prefix = "spring.kafka.consumer", name = ["bootstrap-servers"])
class KafkaConsumerRegisterer(
    private val kafkaProperties: KafkaProperties,
    private val consumers: List<KafkaConsumerDescriptor>,
    private val kafkaModelDeserializer: KafkaModelDeserializer,
    private val customizers: List<ReceiverCustomizer>
) {
    @PostConstruct
    private fun registerConsumers() {
        val consumerProps = kafkaProperties.buildConsumerProperties()

        consumers.forEach { consumer ->
            val defaultOptions = ReceiverOptions
                .create<String, Any>(consumerProps)
                .withValueDeserializer(kafkaModelDeserializer)
                .subscription(listOf(consumer.topic))

            val customizedOptions = customizers.fold(defaultOptions) { options, customizer ->
                customizer.customize(options)
            }

            stream(customizedOptions)
                .flatMapSequential(consumer.concurrency) { record ->
                    consumer
                        .invoke(record)
                        .toFlux()
                        .retry()
                        .map { record }
                        .switchIfEmpty(Flux.just(record))
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
