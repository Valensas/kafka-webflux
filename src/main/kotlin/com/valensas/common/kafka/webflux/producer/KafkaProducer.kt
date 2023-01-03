package com.valensas.common.kafka.webflux.producer

import org.apache.kafka.clients.producer.ProducerRecord
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import reactor.kotlin.core.publisher.toFlux

fun <T> Flux<T>.toKafka(topic: String, producer: KafkaProducer, key: String? = null, partition: Int? = null): Flux<SenderResult<T>> =
    producer.send(topic, this, key, partition)

fun <T> Mono<T>.toKafka(topic: String, producer: KafkaProducer, key: String? = null, partition: Int? = null): Flux<SenderResult<T>> =
    producer.send(topic, this, key, partition)

@Service
class KafkaProducer(
    private val sender: KafkaSender<String, *>
) {
    fun <T> send(topic: String, data: Mono<T>, key: String? = null, partition: Int? = null): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data, key))
    }

    fun <T> send(topic: String, data: Flux<T>, key: String? = null, partition: Int? = null): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data, key))
    }

    fun <K, V> send(record: Publisher<ProducerRecord<K, V>>, partition: Int? = null): Flux<SenderResult<V>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<K, V>
        return record
            .toFlux()
            .map { SenderRecord.create(it, it.value()) }
            .let { sender.send(it) }
    }

    private fun <T> toSenderRecord(topic: String, flux: Flux<T>, key: String? = null, partition: Int? = null): Flux<SenderRecord<String, T, T>> =
        flux.map { SenderRecord.create(ProducerRecord(topic, partition, key, it), it) }

    private fun <T> toSenderRecord(topic: String, flux: Mono<T>, key: String? = null, partition: Int? = null): Mono<SenderRecord<String, T, T>> =
        flux.map { SenderRecord.create(ProducerRecord(topic, partition, key, it), it) }
}
