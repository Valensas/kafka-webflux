package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.consumer.KafkaConsumerDescriptor
import com.valensas.common.kafka.webflux.util.ReceiverCustomizer
import java.util.regex.Pattern
import javax.annotation.PostConstruct
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Deserializer
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kotlin.core.publisher.toFlux

private fun <T, R> Flux<T>.flatMapSequential(concurrent: Boolean, mapper: (T) -> Publisher<R>): Flux<R> =
    if (concurrent)
        flatMapSequential(mapper)
    else
        flatMapSequential(mapper, 1)

@Configuration
@ConditionalOnProperty(prefix = "spring.kafka.consumer", name = ["bootstrap-servers"])
class KafkaConsumerRegisterer(
    private val kafkaProperties: KafkaProperties,
    private val consumers: List<KafkaConsumerDescriptor>,
    private val deserializer: Deserializer<Any>,
    private val customizers: List<ReceiverCustomizer>
) {
    @PostConstruct
    private fun registerConsumers() {
        val consumerProps = kafkaProperties.buildConsumerProperties()

        consumers.forEach { consumer ->
            val defaultOptions = ReceiverOptions
                .create<String, Any>(consumerProps)
                .withValueDeserializer(deserializer)
                .let {
                    if (consumer.wildcard)
                        it.subscription(Pattern.compile(consumer.topic))
                    else
                        it.subscription(listOf(consumer.topic))
                }

            val customizedOptions = customizers.fold(defaultOptions) { options, customizer ->
                customizer.customize(options)
            }

            subscribe(consumer, customizedOptions)
        }
    }

    private fun subscribe(consumer: KafkaConsumerDescriptor, options: ReceiverOptions<String, Any>) {
        stream(options)
            .groupBy(ConsumerRecord<String, Any>::partition)
            .flatMap { partitionFLux ->
                partitionFLux.flatMapSequential(consumer.concurrent) { record ->
                    consumer
                        .invoke(record)
                        .toFlux()
                        .retry()
                        .map { record }
                        .switchIfEmpty(Flux.just(record))
                }
            }
            .doOnNext {
                it.receiverOffset().commit()
            }
            .subscribe()
    }

    private fun stream(options: ReceiverOptions<String, Any>): Flux<ReceiverRecord<String, Any>> =
        KafkaReceiver
            .create(options)
            .receive()
            .onErrorResume {
                stream(options)
            }
}
