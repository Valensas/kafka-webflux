package com.valensas.common.kafka.webflux.autoconfigure

import com.valensas.common.kafka.webflux.consumer.KafkaConsumerDescriptor
import com.valensas.common.kafka.webflux.properties.HeaderPropagationProperties
import com.valensas.common.kafka.webflux.util.ReceiverCustomizer
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Deserializer
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Configuration
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kotlin.core.publisher.toFlux
import java.util.regex.Pattern

private fun <T, R> Flux<T>.flatMapSequential(
    concurrent: Boolean,
    mapper: (T) -> Publisher<R>
): Flux<R> =
    if (concurrent) {
        flatMapSequential(mapper)
    } else {
        flatMapSequential(mapper, 1)
    }

@Configuration
@ConditionalOnProperty(prefix = "spring.kafka.consumer", name = ["bootstrap-servers"])
class KafkaConsumerRegisterer(
    private val kafkaProperties: KafkaProperties,
    private val consumers: List<KafkaConsumerDescriptor>,
    private val deserializer: Deserializer<Any>,
    private val customizers: List<ReceiverCustomizer>
) {
    @Autowired(required = false)
    private var headerPropagationProperties: HeaderPropagationProperties? = null

    private var disposables = emptyList<Disposable>()

    @PostConstruct
    fun registerConsumers() {
        val consumerProps = kafkaProperties.buildConsumerProperties(null)

        disposables =
            consumers.map { consumer ->
                val defaultOptions =
                    ReceiverOptions
                        .create<String, Any>(consumerProps)
                        .withValueDeserializer(deserializer)
                        .let {
                            if (consumer.wildcard) {
                                it.subscription(Pattern.compile(consumer.topic))
                            } else {
                                it.subscription(listOf(consumer.topic))
                            }
                        }

                val customizedOptions =
                    customizers.fold(defaultOptions) { options, customizer ->
                        customizer.customize(options)
                    }

                subscribe(consumer, customizedOptions)
            }
    }

    @PreDestroy
    fun destroy() {
        disposables.forEach(Disposable::dispose)
    }

    private fun subscribe(
        consumer: KafkaConsumerDescriptor,
        options: ReceiverOptions<String, Any>
    ): Disposable =
        stream(options)
            .groupBy(ConsumerRecord<String, Any>::partition)
            .flatMap { partitionFLux ->
                partitionFLux.flatMapSequential(consumer.concurrent) { record ->
                    val headerPropagationProperties = this.headerPropagationProperties

                    consumer
                        .invoke(record)
                        .toFlux()
                        .retry()
                        .map { record }
                        .switchIfEmpty(Flux.just(record))
                        .let {
                            if (headerPropagationProperties == null) return@let it

                            it.contextWrite { context ->
                                val headersMap =
                                    record.headers()
                                        .filter { it.key() in headerPropagationProperties.headers }
                                        .associate { it.key() to it.value().toString(Charsets.UTF_8) }

                                context.put(headerPropagationProperties.contextKey, headersMap)
                            }
                        }
                }
            }
            .flatMap {
                it.receiverOffset().commit()
            }
            .subscribe()

    private fun stream(options: ReceiverOptions<String, Any>): Flux<ReceiverRecord<String, Any>> =
        KafkaReceiver
            .create(options)
            .receive()
            .onErrorResume {
                stream(options)
            }
}
