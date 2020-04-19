package com.valensas.common.kafka.webflux.consumer

import kotlin.reflect.KClass
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Publisher

interface KafkaConsumerDescriptor {
    val topic: String
    val modelType: KClass<*>
    val concurrency: Int?
    fun invoke(record: ConsumerRecord<*, *>): Publisher<Unit>
}
