package com.valensas.common.kafka.webflux.producer

import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult

fun <T> Flux<T>.toKafka(
    topic: String,
    producer: KafkaProducer,
    key: String? = null,
    partition: Int? = null
): Flux<SenderResult<T>> = producer.send(topic, this, key, partition)

fun <T> Mono<T>.toKafka(
    topic: String,
    producer: KafkaProducer,
    key: String? = null,
    partition: Int? = null
): Flux<SenderResult<T>> = producer.send(topic, this, key, partition)

@Service
class KafkaProducer(
    private val sender: KafkaSender<String, *>
) {
    fun <T> send(
        topic: String,
        data: Mono<T>,
        key: String? = null,
        partition: Int? = null
    ): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data, key, partition))
    }

    fun <T> send(
        topic: String,
        data: Flux<T>,
        key: String? = null,
        partition: Int? = null
    ): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data, key, partition))
    }

    private fun <T> toSenderRecord(
        topic: String,
        flux: Flux<T>,
        key: String? = null,
        partition: Int? = null
    ): Flux<SenderRecord<String, T, T>> = flux.map { SenderRecord.create(ProducerRecord(topic, partition, key, it), it) }

    private fun <T> toSenderRecord(
        topic: String,
        flux: Mono<T>,
        key: String? = null,
        partition: Int? = null
    ): Mono<SenderRecord<String, T, T>> = flux.map { SenderRecord.create(ProducerRecord(topic, partition, key, it), it) }
}
