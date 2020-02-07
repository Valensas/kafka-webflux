package com.valensas.common.kafka.webflux.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Publisher
import kotlin.reflect.KClass

interface KafkaConsumerDescriptor {
    val topic: String
    val modelType: KClass<*>
    fun invoke(record: ConsumerRecord<*, *>): Publisher<Unit>
}
