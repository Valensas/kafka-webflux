package com.valensas.common.kafka.webflux.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

interface KafkaConsumerDescriptor {
    val topic: String
    val modelType: KClass<*>
    fun invoke(record: ConsumerRecord<*, *>): Mono<Unit>
}
