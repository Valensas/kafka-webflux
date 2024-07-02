package com.valensas.common.kafka.webflux.producer

import com.valensas.common.kafka.webflux.properties.HeaderPropagationProperties
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import reactor.kotlin.core.publisher.toMono

fun <T : Any> Flux<T>.toKafka(
    topic: String,
    producer: KafkaProducer,
    key: String? = null,
    partition: Int? = null
): Flux<SenderResult<T>> {
    return producer.send(topic, this, key, partition)
}

fun <T : Any> Mono<T>.toKafka(
    topic: String,
    producer: KafkaProducer,
    key: String? = null,
    partition: Int? = null
): Flux<SenderResult<T>> {
    return producer.send(topic, this, key, partition)
}

@Service
class KafkaProducer(
    private val sender: KafkaSender<String, *>
) {
    @Autowired(required = false)
    private val kafkaHeaderPropagationProperties: HeaderPropagationProperties? = null

    fun <T : Any> send(
        topic: String,
        data: Mono<T>,
        key: String? = null,
        partition: Int? = null
    ): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data, key, partition))
    }

    fun <T : Any> send(
        topic: String,
        data: Flux<T>,
        key: String? = null,
        partition: Int? = null
    ): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data, key, partition))
    }

    private fun <T : Any> toSenderRecord(
        topic: String,
        flux: Flux<T>,
        key: String? = null,
        partition: Int? = null
    ): Flux<SenderRecord<String, T, T>> {
        if (kafkaHeaderPropagationProperties == null) {
            return flux.map {
                SenderRecord.create(ProducerRecord(topic, partition, key, it), it)
            }
        }

        val headers =
            this.toMono().transformDeferredContextual { _, context ->
                context.getOrDefault<Map<String, String>>(kafkaHeaderPropagationProperties.contextKey, emptyMap()).toMono()
            }

        return Flux.zip(flux, headers)
            .map {
                val recordHeaders =
                    it.t2.map {
                        RecordHeader(it.key, it.value.toByteArray(Charsets.UTF_8))
                    }
                SenderRecord.create(ProducerRecord(topic, partition, key, it.t1, recordHeaders), it.t1)
            }
    }

    private fun <T : Any> toSenderRecord(
        topic: String,
        flux: Mono<T>,
        key: String? = null,
        partition: Int? = null
    ): Mono<SenderRecord<String, T, T>> {
        if (kafkaHeaderPropagationProperties == null) {
            return flux.map {
                SenderRecord.create(ProducerRecord(topic, partition, key, it), it)
            }
        }

        val headers =
            this.toMono().transformDeferredContextual { _, context ->
                context.getOrDefault<Map<String, String>>(kafkaHeaderPropagationProperties.contextKey, emptyMap()).toMono()
            }

        return Mono.zip(flux, headers)
            .map { zipped ->
                val recordHeaders =
                    zipped.t2.map {
                        RecordHeader(it.key, it.value.toByteArray(Charsets.UTF_8))
                    }
                SenderRecord.create(ProducerRecord(topic, partition, key, zipped.t1, recordHeaders), zipped.t1)
            }
    }
}
