package com.valensas.common.kafka.webflux.consumer

import kotlin.reflect.KClass
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Publisher

class PayloadKafkaConsumerDescriptor<T : Any>(
    override val topic: String,
    override val modelType: KClass<T>,
    override val concurrency: Int?,
    private val consumer: (T) -> Publisher<Unit>
) : KafkaConsumerDescriptor {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(record: ConsumerRecord<*, *>): Publisher<Unit> =
        consumer(record.value() as T)
}
