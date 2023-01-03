package com.valensas.common.kafka.webflux.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Publisher
import kotlin.reflect.KClass

interface KafkaConsumerDescriptor {
    val topic: String
    val wildcard: Boolean
    val modelType: KClass<*>
    val concurrent: Boolean
    fun invoke(record: ConsumerRecord<*, *>): Publisher<Unit>
}
