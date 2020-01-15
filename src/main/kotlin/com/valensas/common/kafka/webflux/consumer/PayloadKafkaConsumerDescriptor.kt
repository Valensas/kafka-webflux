package com.valensas.common.kafka.webflux.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

class PayloadKafkaConsumerDescriptor<T : Any>(
    override val topic: String,
    override val modelType: KClass<T>,
    private val consumer: (T) -> Mono<Unit>
) : KafkaConsumerDescriptor {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(record: ConsumerRecord<*, *>): Mono<Unit> =
        consumer(record.value() as T)
}
