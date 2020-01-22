
package com.valensas.common.kafka.webflux.producer

import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult

fun <T> Flux<T>.toKafka(topic: String, producer: KafkaProducer): Flux<SenderResult<T>> =
    producer.send(topic, this)

fun <T> Mono<T>.toKafka(topic: String, producer: KafkaProducer): Flux<SenderResult<T>> =
    producer.send(topic, this)

@Service
class KafkaProducer(
    private val sender: KafkaSender<String, *>
) {
    fun <T> send(topic: String, data: Mono<T>): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data))
    }

    fun <T> send(topic: String, data: Flux<T>): Flux<SenderResult<T>> {
        @Suppress("UNCHECKED_CAST")
        val sender = this.sender as KafkaSender<String, T>
        return sender.send(toSenderRecord(topic, data))
    }

    private fun <T> toSenderRecord(topic: String, flux: Flux<T>): Flux<SenderRecord<String, T, T>> =
        flux.map { SenderRecord.create(ProducerRecord<String, T>(topic, it), it) }

    private fun <T> toSenderRecord(topic: String, flux: Mono<T>): Mono<SenderRecord<String, T, T>> =
        flux.map { SenderRecord.create(ProducerRecord<String, T>(topic, it), it) }
}
